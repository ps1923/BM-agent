package com.bmhs.teacher;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.auth.AuthContext;
import com.bmhs.experimentcreation.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class TeacherEvidenceService {
    private static final int MAX_FILES = 500;
    private static final int MAX_ENTRIES = 2_000;
    private static final Pattern SNAPSHOT_KEY = Pattern.compile("\\.node-evidence/([0-9a-fA-F-]{36})");

    private final TeacherRepository repository;
    private final Path configuredRoot;
    private final long maxFileBytes;
    private final long maxSnapshotBytes;

    public TeacherEvidenceService(TeacherRepository repository,
                                  @Value("${bm-hs.workspace.root-path:/var/lib/bm-hs/workspaces}") String rootPath,
                                  @Value("${bm-hs.workspace.max-file-bytes:2000000}") long maxFileBytes,
                                  @Value("${bm-hs.workspace.max-snapshot-bytes:20000000}") long maxSnapshotBytes) {
        this.repository = repository;
        this.configuredRoot = Path.of(rootPath).toAbsolutePath().normalize();
        this.maxFileBytes = maxFileBytes;
        this.maxSnapshotBytes = maxSnapshotBytes;
    }

    public List<TeacherModels.StageEvidence> stages(AuthenticatedUser user, long runId) {
        AuthContext.requireRole(user, "teacher");
        return repository.stageEvidence(user.id(), runId);
    }

    public List<TeacherModels.SnapshotFile> files(AuthenticatedUser user, long runId, long snapshotId) {
        AuthContext.requireRole(user, "teacher");
        TeacherRepository.SnapshotRecord snapshot = repository.assertSnapshotAccess(user.id(), runId, snapshotId);
        Path snapshotRoot = snapshotRoot(snapshot);
        List<TeacherModels.SnapshotFile> result = new ArrayList<>();
        long totalBytes = 0;
        int entries = 0;
        try (Stream<Path> paths = Files.walk(snapshotRoot)) {
            var iterator = paths.filter(path -> !path.equals(snapshotRoot)).iterator();
            while (iterator.hasNext()) {
                Path file = iterator.next();
                if (++entries > MAX_ENTRIES) {
                    throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "SNAPSHOT_TOO_LARGE", "阶段代码证据目录项过多");
                }
                if (Files.isSymbolicLink(file)) throw invalidSnapshot();
                if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) continue;
                long size = Files.size(file);
                totalBytes = Math.addExact(totalBytes, size);
                if (size > maxFileBytes || totalBytes > maxSnapshotBytes || result.size() >= MAX_FILES) {
                    throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "SNAPSHOT_TOO_LARGE", "阶段代码证据超过读取限制");
                }
                String relative = normalize(snapshotRoot.relativize(file).toString());
                result.add(new TeacherModels.SnapshotFile(relative, size, isText(file)));
            }
        } catch (ApiException exception) {
            throw exception;
        } catch (IOException | ArithmeticException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SNAPSHOT_READ_FAILED", "阶段代码证据读取失败");
        }
        result.sort(Comparator.comparing(TeacherModels.SnapshotFile::path));
        return List.copyOf(result);
    }

    public TeacherModels.SnapshotFileContent readFile(AuthenticatedUser user, long runId, long snapshotId,
                                                       String rawPath) {
        AuthContext.requireRole(user, "teacher");
        TeacherRepository.SnapshotRecord snapshot = repository.assertSnapshotAccess(user.id(), runId, snapshotId);
        Path snapshotRoot = snapshotRoot(snapshot);
        String relative = normalize(rawPath);
        Path file = snapshotRoot.resolve(relative).normalize();
        if (!file.startsWith(snapshotRoot)) throw invalidSnapshot();
        rejectSymlinkComponents(snapshotRoot, file);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw fileNotFound();
        try {
            long size = Files.size(file);
            if (size > maxFileBytes || size > maxSnapshotBytes) {
                throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "SNAPSHOT_FILE_TOO_LARGE", "文件超过读取限制");
            }
            byte[] bytes = readBounded(file, Math.min(maxFileBytes, maxSnapshotBytes));
            String content = decodeText(bytes);
            return new TeacherModels.SnapshotFileContent(new TeacherModels.SnapshotFile(relative, bytes.length, true), content);
        } catch (ApiException exception) {
            throw exception;
        } catch (IOException exception) {
            throw fileNotFound();
        }
    }

    private Path snapshotRoot(TeacherRepository.SnapshotRecord snapshot) {
        var matcher = SNAPSHOT_KEY.matcher(snapshot.storageKey());
        if (!matcher.matches()) throw invalidSnapshot();
        Path workspaceRoot = Path.of(snapshot.storagePath()).toAbsolutePath().normalize();
        Path evidenceRoot = workspaceRoot.resolveSibling(workspaceRoot.getFileName() + ".node-evidence").normalize();
        Path root = evidenceRoot.resolve(matcher.group(1)).normalize();
        if (!workspaceRoot.startsWith(configuredRoot) || !evidenceRoot.startsWith(configuredRoot)
                || !root.startsWith(evidenceRoot)) throw invalidSnapshot();
        try {
            Path realConfigured = configuredRoot.toRealPath();
            rejectSymlinkComponents(configuredRoot.getRoot(), evidenceRoot);
            rejectSymlinkComponents(configuredRoot.getRoot(), workspaceRoot);
            Path realWorkspace = workspaceRoot.toRealPath();
            Path realEvidenceRoot = evidenceRoot.toRealPath();
            if (!realWorkspace.startsWith(realConfigured)) throw invalidSnapshot();
            if (!realEvidenceRoot.startsWith(realConfigured)) throw invalidSnapshot();
            rejectSymlinkComponents(evidenceRoot, root);
            if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) throw fileNotFound();
        } catch (IOException exception) {
            throw fileNotFound();
        }
        return root;
    }

    private void rejectSymlinkComponents(Path root, Path file) {
        Path current = root;
        for (Path component : root.relativize(file)) {
            current = current.resolve(component);
            if (Files.isSymbolicLink(current)) throw invalidSnapshot();
        }
    }

    private String normalize(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) throw invalidSnapshot();
        String path = rawPath.replace('\\', '/');
        if (path.startsWith("/") || path.matches("^[A-Za-z]:.*") || path.contains("../")
                || path.equals("..") || path.contains("/..") || path.indexOf('\0') >= 0) throw invalidSnapshot();
        return path;
    }

    private boolean isText(Path file) throws IOException {
        long size = Files.size(file);
        if (size > maxFileBytes) return false;
        byte[] bytes = readBounded(file, maxFileBytes);
        try {
            decodeText(bytes);
            return true;
        } catch (ApiException exception) {
            return false;
        } catch (CharacterCodingException exception) {
            return false;
        }
    }

    private String decodeText(byte[] bytes) throws CharacterCodingException {
        String content = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        for (int offset = 0; offset < content.length();) {
            int codePoint = content.codePointAt(offset);
            if (Character.isISOControl(codePoint) && codePoint != '\t' && codePoint != '\n'
                    && codePoint != '\r' && codePoint != '\f') throw unsupportedFile();
            offset += Character.charCount(codePoint);
        }
        return content;
    }

    private byte[] readBounded(Path file, long maxBytes) throws IOException {
        long boundedLimit = Math.min(maxBytes, Integer.MAX_VALUE - 1L);
        try (InputStream input = Files.newInputStream(file); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer, 0, (int) Math.min(buffer.length, boundedLimit + 1 - output.size()))) != -1) {
                if (output.size() + read > boundedLimit) {
                    throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "SNAPSHOT_FILE_TOO_LARGE", "文件超过读取限制");
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private ApiException invalidSnapshot() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SNAPSHOT_PATH", "阶段代码证据路径无效");
    }

    private ApiException fileNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "SNAPSHOT_FILE_NOT_FOUND", "阶段代码文件不存在");
    }

    private ApiException unsupportedFile() {
        return new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "SNAPSHOT_FILE_NOT_TEXT", "该文件不是可安全展示的 UTF-8 文本");
    }
}
