package com.bmhs.workspace;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.experimentcreation.ApiException;
import com.bmhs.workspace.WorkspaceModels.RunRecord;
import com.bmhs.workspace.WorkspaceModels.RunView;
import com.bmhs.workspace.WorkspaceModels.WorkspaceFile;
import com.bmhs.workspace.WorkspaceModels.WorkspaceFileContent;
import com.bmhs.workspace.WorkspaceModels.WorkspaceRecord;
import com.bmhs.workspace.WorkspaceModels.RunProgressView;
import com.bmhs.workspace.WorkspaceModels.SnapshotView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.core.Ordered;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.DirectoryStream;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Comparator;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.bmhs.patch.PatchModels;

@Service
public class WorkspaceService {
    private static final Logger log = LoggerFactory.getLogger(WorkspaceService.class);
    private static final int MAX_SNAPSHOT_FILES = 500;
    private static final int MAX_SNAPSHOT_ENTRIES = 2_000;
    private static final Set<String> SNAPSHOT_EXCLUDED_DIRECTORIES = Set.of(
            ".snapshots", ".terminal", ".git", ".idea", "target", "build", "dist",
            "node_modules", "__pycache__", ".venv", "venv", "out");
    private static final int MAX_INDEXED_WORKSPACE_FILES = 500;
    private static final int MAX_INDEXED_WORKSPACE_ENTRIES = 2_000;
    private static final int MAX_INDEXED_WORKSPACE_DEPTH = 32;
    private static final Set<String> WORKSPACE_INDEX_EXCLUDED_DIRECTORIES = Set.of(
            ".snapshots", ".node-evidence", ".manual-snapshots", ".terminal", ".git", ".idea",
            "target", "build", "dist", "out", "node_modules", "__pycache__", ".venv", "venv",
            ".pytest_cache", ".mypy_cache", ".ruff_cache", ".gradle", ".m2", ".cache",
            "site-packages", "__pypackages__", "vendor", "coverage");
    private static final Set<String> WORKSPACE_INDEXABLE_EXTENSIONS = Set.of(
            "java", "py", "xml", "yml", "yaml", "properties", "toml", "json", "md", "txt",
            "sql", "sh", "gradle", "kts", "ini", "cfg", "conf", "html", "css");
    private static final Set<String> WORKSPACE_INDEXABLE_NAMES = Set.of(
            "dockerfile", "makefile", "requirements.txt", "license", "readme");
    private static final Pattern SENSITIVE_CONFIG_VALUE = Pattern.compile(
            "(?im)([\\\"']?[\\w.-]*(?:password|passwd|secret|token|api[-_]?key|access[-_]?key|private[-_]?key|credential)[\\w.-]*[\\\"']?\\s*[:=]\\s*)([^\\r\\n]*)");
    private static final Pattern URI_CREDENTIALS = Pattern.compile("(?i)(://[^:/@\\s]+:)[^@/\\s]+@");
    private static final Pattern BEARER_CREDENTIAL = Pattern.compile("(?i)\\bBearer\\s+[A-Za-z0-9._~+/-]+=*");
    private static final Pattern CLOUD_ACCESS_KEY = Pattern.compile("\\b(?:AKIA|ASIA)[A-Z0-9]{16}\\b");
    private final WorkspaceRepository repository;
    private final WorkspaceTemplateService templateService;
    private final WorkspaceRuntime runtime;
    private final WorkspaceModels.WorkspaceConfig config;
    private final int maxSnapshotBytes;
    private final boolean requireSecureDirectoryStream;
    private final Object pausedRuntimeResourceKey = new Object();

    @Autowired
    public WorkspaceService(
            WorkspaceRepository repository,
            WorkspaceTemplateService templateService,
            WorkspaceRuntime runtime,
            @Value("${bm-hs.workspace.root-path:/var/lib/bm-hs/workspaces}") String rootPath,
            @Value("${bm-hs.workspace.runtime-image:bm-hs/workspace:latest}") String runtimeImage,
            @Value("${bm-hs.workspace.cpu-limit:1.0}") java.math.BigDecimal cpuLimit,
            @Value("${bm-hs.workspace.memory-limit-mb:1024}") int memoryLimitMb,
            @Value("${bm-hs.workspace.max-file-bytes:2000000}") int maxFileBytes,
            @Value("${bm-hs.workspace.max-snapshot-bytes:20000000}") int maxSnapshotBytes,
            @Value("${bm-hs.workspace.require-secure-directory-stream:true}") boolean requireSecureDirectoryStream) {
        if (cpuLimit.signum() <= 0 || memoryLimitMb <= 0 || maxFileBytes <= 0 || maxSnapshotBytes <= 0) {
            throw new IllegalArgumentException("工作区资源限制必须大于 0");
        }
        this.repository = repository;
        this.templateService = templateService;
        this.runtime = runtime;
        this.config = new WorkspaceModels.WorkspaceConfig(rootPath, runtimeImage, cpuLimit, memoryLimitMb, maxFileBytes);
        this.maxSnapshotBytes = maxSnapshotBytes;
        this.requireSecureDirectoryStream = requireSecureDirectoryStream;
    }

    WorkspaceService(WorkspaceRepository repository, WorkspaceRuntime runtime,
                     String rootPath, String runtimeImage, java.math.BigDecimal cpuLimit,
                     int memoryLimitMb, int maxFileBytes, int maxSnapshotBytes) {
        this(repository, new WorkspaceTemplateService(repository), runtime, rootPath, runtimeImage,
                cpuLimit, memoryLimitMb, maxFileBytes, maxSnapshotBytes, false);
    }

    public RunView createRun(AuthenticatedUser user, long experimentId) {
        requireStudent(user);
        RunRecord run = repository.createOrFindRun(experimentId, user.id(), config);
        if ("error".equals(run.workspace().status()) && run.workspace().runtimeInstanceId() == null) {
            run = repository.retryProvisioning(run.id(), user.id());
        }
        WorkspaceRecord workspace = run.workspace();
        if ("running".equals(workspace.status())) {
            try {
                ensureTemplate(workspace);
            } catch (RuntimeException exception) {
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "WORKSPACE_TEMPLATE_FAILED",
                        "工作区模板初始化失败，请稍后重试");
            }
        }
        if ("provisioning".equals(workspace.status())) {
            if (!repository.claimWorkspaceProvisioning(workspace.id())) {
                return repository.toView(repository.findRun(run.id(), user.id()));
            }
            String instanceId = null;
            try {
                ensureTemplate(workspace);
                instanceId = runtime.start(workspace);
                repository.updateRuntime(workspace.id(), "running", instanceId);
                run = repository.findRun(run.id(), user.id());
            } catch (RuntimeException exception) {
                if (instanceId != null) {
                    try {
                        runtime.stop(instanceId);
                    } catch (RuntimeException cleanupFailure) {
                        exception.addSuppressed(cleanupFailure);
                        log.error("Could not remove an unregistered workspace runtime", cleanupFailure);
                    }
                }
                repository.updateRuntime(workspace.id(), "error", null);
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "WORKSPACE_PROVISION_FAILED",
                        "工作区启动失败，请稍后重试");
            }
        }
        return repository.toView(run);
    }

    private void ensureTemplate(WorkspaceRecord workspace) {
        Path root = safeRoot(workspace.storagePath());
        try {
            Files.createDirectories(root);
        } catch (IOException exception) {
            throw new IllegalStateException("工作区目录创建失败", exception);
        }
        String experimentName = repository.findExperimentName(workspace.experimentId());
        if (experimentName != null) {
            templateService.materializeIfEmpty(workspace, experimentName, config.maxFileBytes());
        }
    }

    public RunView getRun(AuthenticatedUser user, long runId) {
        requireStudent(user);
        return repository.toView(repository.findRun(runId, user.id()));
    }

    public RunProgressView getProgress(AuthenticatedUser user, long runId) {
        requireStudent(user);
        return repository.findProgress(runId, user.id());
    }

    @Transactional
    public RunProgressView updateNode(AuthenticatedUser user, long runId, long nodeId,
                                      WorkspaceModels.UpdateNodeRequest request) {
        requireStudent(user);
        RunRecord run = repository.findRun(runId, user.id());
        repository.lockWorkspaceForMutation(run.workspace().id());
        RunProgressView before = repository.findProgress(runId, user.id());
        boolean newlyCompleting = "completed".equals(request.status()) && before.nodes().stream()
                .anyMatch(node -> node.nodeId() == nodeId && !"completed".equals(node.status()));
        RunProgressView progress = repository.updateNodeProgress(runId, user.id(), nodeId,
                request.status(), request.progressPercent());
        if (newlyCompleting) {
            WorkspaceRecord workspace = run.workspace();
            withWorkspacePaused(workspace, () -> {
                ensureNodeCompletionSnapshot(workspace, nodeId);
                return null;
            });
        }
        return progress;
    }

    private void ensureNodeCompletionSnapshot(WorkspaceRecord workspace, long nodeId) {
        if (!repository.findNodeCompletionSnapshots(workspace.id(), nodeId).isEmpty()) return;
        Path root = safeRoot(workspace.storagePath());
        String snapshotId = UUID.randomUUID().toString();
        Path evidenceRoot = root.resolveSibling(root.getFileName() + ".node-evidence").normalize();
        Path stagingRoot = evidenceRoot.resolve(".staging-" + snapshotId).normalize();
        Path finalRoot = evidenceRoot.resolve(snapshotId).normalize();
        Path configuredRoot = Path.of(config.rootPath()).toAbsolutePath().normalize();
        if (!evidenceRoot.startsWith(configuredRoot) || !stagingRoot.startsWith(evidenceRoot)
                || !finalRoot.startsWith(evidenceRoot)) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SNAPSHOT_PATH_INVALID", "快照路径无效");
        }
        try {
            rejectSymlinkComponents(root.getRoot(), root);
            rejectSymlinkComponents(configuredRoot, evidenceRoot);
            Files.createDirectories(evidenceRoot);
            Files.createDirectory(stagingRoot);
            copyWorkspaceEvidence(root, stagingRoot);
            Files.move(stagingRoot, finalRoot, StandardCopyOption.ATOMIC_MOVE);
            String storageKey = ".node-evidence/" + snapshotId;
            SnapshotView persisted = repository.createNodeCompletionSnapshot(
                    workspace.id(), workspace.experimentId(), nodeId, storageKey);
            if (!storageKey.equals(persisted.storageKey())) deleteSnapshotTree(finalRoot);
            registerSnapshotRollbackCleanup(finalRoot);
        } catch (SnapshotTooLargeException exception) {
            deleteSnapshotTree(stagingRoot);
            deleteSnapshotTree(finalRoot);
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "SNAPSHOT_TOO_LARGE", "阶段代码证据超过文件数或大小限制");
        } catch (UnsupportedSnapshotTextException exception) {
            deleteSnapshotTree(stagingRoot);
            deleteSnapshotTree(finalRoot);
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "SNAPSHOT_FILE_NOT_TEXT", "阶段代码证据包含无法安全展示的文件");
        } catch (IOException | ArithmeticException exception) {
            deleteSnapshotTree(stagingRoot);
            deleteSnapshotTree(finalRoot);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SNAPSHOT_CREATE_FAILED", "阶段代码证据保存失败");
        } catch (RuntimeException exception) {
            deleteSnapshotTree(stagingRoot);
            deleteSnapshotTree(finalRoot);
            throw exception;
        }
    }

    private void copyWorkspaceEvidence(Path workspaceRoot, Path stagingRoot) throws IOException {
        long[] totalBytes = {0};
        int[] entries = {0};
        int[] copiedFiles = {0};
        Files.walkFileTree(workspaceRoot, Set.of(), 32, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) throws IOException {
                if (attributes.isSymbolicLink()) throw new IOException("工作区不允许包含符号链接");
                if (!directory.equals(workspaceRoot)
                        && SNAPSHOT_EXCLUDED_DIRECTORIES.contains(directory.getFileName().toString().toLowerCase(Locale.ROOT))) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                if (++entries[0] > MAX_SNAPSHOT_ENTRIES) throw new SnapshotTooLargeException();
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                if (++entries[0] > MAX_SNAPSHOT_ENTRIES) throw new SnapshotTooLargeException();
                if (attributes.isSymbolicLink()) throw new IOException("工作区不允许包含符号链接");
                if (attributes.isDirectory()) throw new IOException("工作区目录层级超过快照限制");
                if (!attributes.isRegularFile() || !isEvidenceTextPath(file)) return FileVisitResult.CONTINUE;
                if (++copiedFiles[0] > MAX_SNAPSHOT_FILES) throw new SnapshotTooLargeException();
                long size = attributes.size();
                if (size > config.maxFileBytes()) throw new SnapshotTooLargeException();
                totalBytes[0] = Math.addExact(totalBytes[0], size);
                if (totalBytes[0] > maxSnapshotBytes) throw new SnapshotTooLargeException();
                byte[] bytes = readStableWorkspaceFile(workspaceRoot, file, size, config.maxFileBytes());
                bytes = sanitizeSnapshotText(bytes);
                Path relative = workspaceRoot.relativize(file).normalize();
                Path destination = stagingRoot.resolve(relative).normalize();
                if (!destination.startsWith(stagingRoot)) throw new IOException("快照目标路径越界");
                Files.createDirectories(destination.getParent());
                Files.write(destination, bytes);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private boolean isEvidenceTextPath(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (Set.of("dockerfile", "makefile", ".gitignore", ".dockerignore").contains(name)) return true;
        int dot = name.lastIndexOf('.');
        if (dot < 0) return false;
        // Machine configuration frequently carries credentials; omit it from immutable teacher snapshots.
        return Set.of("java", "py", "js", "jsx", "ts", "tsx", "c", "h", "cc", "cpp", "hpp",
                "go", "rs", "rb", "php", "vue", "kt", "kts", "scala", "swift", "cs", "m", "mm",
                "md", "txt", "html", "css", "sql", "sh", "bat", "ps1").contains(name.substring(dot + 1))
                && !isSensitiveEvidenceName(name);
    }

    private boolean isSensitiveEvidenceName(String name) {
        return name.equals(".env") || name.startsWith(".env.")
                || name.contains("secret") || name.contains("credential")
                || name.endsWith(".pem") || name.endsWith(".key");
    }

    private byte[] sanitizeSnapshotText(byte[] bytes) throws IOException {
        String content;
        try {
            content = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(bytes)).toString();
        } catch (java.nio.charset.CharacterCodingException exception) {
            throw new UnsupportedSnapshotTextException("源码文件不是有效 UTF-8 文本", exception);
        }
        content = redactCommonSecrets(content);
        for (int offset = 0; offset < content.length();) {
            int codePoint = content.codePointAt(offset);
            if (Character.isISOControl(codePoint) && codePoint != '\t' && codePoint != '\n'
                    && codePoint != '\r' && codePoint != '\f') {
                throw new UnsupportedSnapshotTextException("源码文件包含不可展示控制字符");
            }
            offset += Character.charCount(codePoint);
        }
        return content.getBytes(StandardCharsets.UTF_8);
    }

    public static String redactCommonSecrets(String content) {
        Matcher sensitiveValue = SENSITIVE_CONFIG_VALUE.matcher(content);
        StringBuffer sanitized = new StringBuffer();
        while (sensitiveValue.find()) {
            sensitiveValue.appendReplacement(sanitized,
                    Matcher.quoteReplacement(sensitiveValue.group(1) + "[REDACTED]"));
        }
        sensitiveValue.appendTail(sanitized);
        String redacted = URI_CREDENTIALS.matcher(sanitized).replaceAll("$1[REDACTED]@");
        redacted = BEARER_CREDENTIAL.matcher(redacted).replaceAll("Bearer [REDACTED]");
        return CLOUD_ACCESS_KEY.matcher(redacted).replaceAll("[REDACTED]");
    }

    private void registerSnapshotRollbackCleanup(Path snapshotRoot) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) deleteSnapshotTree(snapshotRoot);
            }
        });
    }

    private byte[] readBounded(Path file, int maxBytes) throws IOException {
        try (InputStream input = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
            return readBounded(input, maxBytes);
        }
    }

    private byte[] readBounded(InputStream input, int maxBytes) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer, 0,
                    (int) Math.min(buffer.length, (long) maxBytes + 1 - output.size()))) != -1) {
                if (output.size() + read > maxBytes) throw new SnapshotTooLargeException();
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private byte[] readStableWorkspaceFile(Path workspaceRoot, Path file, long expectedSize, int maxBytes)
            throws IOException {
        if (requireSecureDirectoryStream) {
            try (SecureWorkspaceParent parent = openSecureWorkspaceParent(
                    workspaceRoot, workspaceRoot.relativize(file).toString())) {
                return readSecureWorkspaceFile(parent, expectedSize, maxBytes);
            }
        }
        rejectSymlinkComponents(workspaceRoot, file);
        BasicFileAttributes before = Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!before.isRegularFile() || before.isSymbolicLink() || before.size() != expectedSize) {
            throw new IOException("工作区文件在快照期间发生变化");
        }
        byte[] bytes = readBounded(file, maxBytes);
        if (!Arrays.equals(bytes, readBounded(file, maxBytes))) {
            throw new IOException("工作区文件读取期间内容发生变化");
        }
        rejectSymlinkComponents(workspaceRoot, file);
        BasicFileAttributes after = Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!after.isRegularFile() || after.isSymbolicLink() || after.size() != expectedSize
                || bytes.length != after.size()
                || (before.fileKey() != null && !before.fileKey().equals(after.fileKey()))
                || !before.lastModifiedTime().equals(after.lastModifiedTime())) {
            throw new IOException("工作区文件在快照期间发生变化");
        }
        return bytes;
    }

    private byte[] readWorkspaceFile(Path workspaceRoot, String relativePath, int maxBytes) throws IOException {
        if (!requireSecureDirectoryStream) return readBounded(workspaceRoot.resolve(relativePath), maxBytes);
        try (SecureWorkspaceParent parent = openSecureWorkspaceParent(workspaceRoot, relativePath)) {
            return readSecureWorkspaceFile(parent, -1, maxBytes);
        }
    }

    private byte[] readSecureWorkspaceFile(SecureWorkspaceParent parent, long expectedSize, int maxBytes)
            throws IOException {
        BasicFileAttributeView attributes = parent.directory().getFileAttributeView(
                parent.fileName(), BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        BasicFileAttributes before = attributes.readAttributes();
        if (!before.isRegularFile() || before.isSymbolicLink()
                || (expectedSize >= 0 && before.size() != expectedSize)
                || before.size() > maxBytes
                || (requireSecureDirectoryStream && before.fileKey() == null)) {
            throw new IOException("工作区文件类型、身份或大小不符合限制");
        }
        byte[] bytes;
        try (SeekableByteChannel channel = parent.directory().newByteChannel(parent.fileName(),
                Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS))) {
            bytes = readBounded(Channels.newInputStream(channel), maxBytes);
            channel.position(0);
            if (!Arrays.equals(bytes, readBounded(Channels.newInputStream(channel), maxBytes))) {
                throw new IOException("工作区文件读取期间内容发生变化");
            }
        }
        BasicFileAttributes after = attributes.readAttributes();
        if (!after.isRegularFile() || after.isSymbolicLink() || after.size() != bytes.length
                || (expectedSize >= 0 && after.size() != expectedSize)
                || (requireSecureDirectoryStream && after.fileKey() == null)
                || (before.fileKey() != null && !before.fileKey().equals(after.fileKey()))
                || !before.lastModifiedTime().equals(after.lastModifiedTime())) {
            throw new IOException("工作区文件读取期间发生变化");
        }
        return bytes;
    }

    private SecureWorkspaceParent openSecureWorkspaceParent(Path workspaceRoot, String relativePath)
            throws IOException {
        Path relative = Path.of(relativePath).normalize();
        if (relative.isAbsolute() || relative.getNameCount() == 0 || relative.startsWith("..")) {
            throw new IOException("工作区文件路径越界");
        }
        BasicFileAttributes expectedRoot = Files.readAttributes(
                workspaceRoot, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!expectedRoot.isDirectory() || expectedRoot.isSymbolicLink() || expectedRoot.fileKey() == null) {
            throw new IOException("工作区根目录身份无法确认");
        }
        DirectoryStream<Path> rootDirectory = Files.newDirectoryStream(workspaceRoot);
        if (!(rootDirectory instanceof SecureDirectoryStream<?> secureDirectory)) {
            rootDirectory.close();
            throw new IOException("安全工作区操作需要 SecureDirectoryStream 支持");
        }
        @SuppressWarnings("unchecked")
        SecureDirectoryStream<Path> current = (SecureDirectoryStream<Path>) secureDirectory;
        List<SecureDirectoryStream<Path>> openedDirectories = new java.util.ArrayList<>();
        openedDirectories.add(current);
        try {
            BasicFileAttributes openedRoot = current.getFileAttributeView(BasicFileAttributeView.class)
                    .readAttributes();
            if (!expectedRoot.fileKey().equals(openedRoot.fileKey())) {
                throw new IOException("工作区根目录在打开期间发生变化");
            }
            for (int index = 0; index < relative.getNameCount() - 1; index++) {
                Path component = relative.getName(index);
                // SecureDirectoryStream has no mkdir-at operation; don't fall back to a
                // pathname-based create that would reopen the symlink race.
                SecureDirectoryStream<Path> child = current.newDirectoryStream(component, LinkOption.NOFOLLOW_LINKS);
                current = child;
                openedDirectories.add(current);
            }
            return new SecureWorkspaceParent(current, relative.getFileName(), openedDirectories);
        } catch (IOException | RuntimeException exception) {
            closeSecureDirectories(openedDirectories);
            throw exception;
        }
    }

    private record SecureWorkspaceParent(SecureDirectoryStream<Path> directory, Path fileName,
                                         List<SecureDirectoryStream<Path>> openedDirectories)
            implements AutoCloseable {
        @Override
        public void close() {
            for (int index = openedDirectories.size() - 1; index >= 0; index--) {
                try {
                    openedDirectories.get(index).close();
                } catch (IOException ignored) {
                    // Closing a handle does not alter the saved workspace data.
                }
            }
        }
    }

    private void closeSecureDirectories(List<SecureDirectoryStream<Path>> directories) {
        for (int index = directories.size() - 1; index >= 0; index--) {
            try {
                directories.get(index).close();
            } catch (IOException exception) {
                log.warn("Could not close workspace directory handle", exception);
            }
        }
    }

    private void rejectSymlinkComponents(Path root, Path file) throws IOException {
        Path current = root;
        for (Path component : root.relativize(file)) {
            current = current.resolve(component);
            if (Files.isSymbolicLink(current)) throw new IOException("不允许快照符号链接");
        }
    }

    public RunProgressView updateTask(AuthenticatedUser user, long runId, long taskId,
                                      WorkspaceModels.UpdateTaskRequest request) {
        requireStudent(user);
        return repository.updateTaskProgress(runId, user.id(), taskId, request.status());
    }

    @Transactional
    public List<WorkspaceFile> listFiles(AuthenticatedUser user, long workspaceId) {
        WorkspaceRecord workspace = accessibleWorkspace(user, workspaceId);
        repository.lockWorkspaceForMutation(workspaceId);
        Path root = safeRoot(workspace.storagePath());
        return refreshIndexedFiles(workspaceId, root);
    }

    private List<WorkspaceFile> refreshIndexedFiles(long workspaceId, Path root) {
        List<WorkspaceFile> indexedFiles = repository.listFiles(workspaceId);
        if (indexedFiles == null) indexedFiles = List.of();
        Set<String> existingPaths = new HashSet<>();
        Map<String, WorkspaceFile> indexedByPath = new HashMap<>();
        for (WorkspaceFile indexedFile : indexedFiles) {
            existingPaths.add(indexedFile.path());
            indexedByPath.put(indexedFile.path(), indexedFile);
        }

        List<IndexedWorkspaceFile> discoveredFiles = scanWorkspaceFiles(root);
        discoveredFiles.sort(Comparator.comparing(IndexedWorkspaceFile::path));
        Set<String> discoveredPaths = new HashSet<>();
        int nextSortOrder = repository.nextWorkspaceFileSortOrder(workspaceId);
        for (IndexedWorkspaceFile file : discoveredFiles) {
            discoveredPaths.add(file.path());
            WorkspaceFile existing = indexedByPath.get(file.path());
            if (existing == null || existing.sizeBytes() != file.sizeBytes()
                    || !file.contentHash().equalsIgnoreCase(existing.contentHash())) {
                repository.upsertIndexedFile(workspaceId, file.path(), displayName(file.path()), language(file.path()),
                        file.sizeBytes(), file.contentHash(), existing == null ? nextSortOrder++ : 0);
            }
        }
        for (String existingPath : existingPaths) {
            if (!discoveredPaths.contains(existingPath)) repository.deleteIndexedFile(workspaceId, existingPath);
        }
        List<WorkspaceFile> refreshedFiles = repository.listFiles(workspaceId);
        return refreshedFiles == null ? List.of() : refreshedFiles;
    }

    private List<IndexedWorkspaceFile> scanWorkspaceFiles(Path root) {
        List<IndexedWorkspaceFile> discovered = new java.util.ArrayList<>();
        int[] entries = {0};
        long[] totalBytes = {0};
        try {
            BasicFileAttributes rootAttributes = Files.readAttributes(
                    root, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!rootAttributes.isDirectory() || rootAttributes.isSymbolicLink()) {
                throw new IOException("工作区根目录无效");
            }
            Files.walkFileTree(root, Set.of(), MAX_INDEXED_WORKSPACE_DEPTH, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
                        throws IOException {
                    countWorkspaceIndexEntry(entries);
                    if (attributes.isSymbolicLink()) return FileVisitResult.SKIP_SUBTREE;
                    if (!directory.equals(root) && WORKSPACE_INDEX_EXCLUDED_DIRECTORIES.contains(
                            directory.getFileName().toString().toLowerCase(Locale.ROOT))) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    countWorkspaceIndexEntry(entries);
                    if (!attributes.isRegularFile() || attributes.isSymbolicLink()) return FileVisitResult.CONTINUE;
                    String relativePath = root.relativize(file).normalize().toString().replace('\\', '/');
                    if (relativePath.length() > 700 || !isWorkspaceIndexablePath(relativePath)
                            || attributes.size() > config.maxFileBytes()) {
                        return FileVisitResult.CONTINUE;
                    }
                    if (discovered.size() >= MAX_INDEXED_WORKSPACE_FILES) throw new WorkspaceIndexLimitException();
                    if (totalBytes[0] + attributes.size() > maxSnapshotBytes) throw new WorkspaceIndexLimitException();
                    byte[] content;
                    try {
                        content = readStableWorkspaceFile(root, file, attributes.size(), config.maxFileBytes());
                    } catch (SnapshotTooLargeException | java.nio.file.NoSuchFileException changedDuringScan) {
                        return FileVisitResult.CONTINUE;
                    }
                    if (!isUtf8Text(content)) return FileVisitResult.CONTINUE;
                    totalBytes[0] += content.length;
                    discovered.add(new IndexedWorkspaceFile(relativePath, content.length, sha256(content)));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exception) {
                    return FileVisitResult.CONTINUE;
                }
            });
            return discovered;
        } catch (WorkspaceIndexLimitException exception) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "WORKSPACE_INDEX_LIMIT",
                    "工作区源码文件数量或总体积超过编辑器索引上限");
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "WORKSPACE_FILE_INDEX_FAILED",
                    "工作区文件索引刷新失败，请稍后重试");
        }
    }

    private void countWorkspaceIndexEntry(int[] entries) throws WorkspaceIndexLimitException {
        if (++entries[0] > MAX_INDEXED_WORKSPACE_ENTRIES) throw new WorkspaceIndexLimitException();
    }

    private boolean isWorkspaceIndexablePath(String relativePath) {
        try {
            String normalized = normalizePath(relativePath);
            rejectReservedWorkspacePath(normalized);
            String fileName = displayName(normalized).toLowerCase(Locale.ROOT);
            if (isSensitiveEvidenceName(fileName)) return false;
            if (WORKSPACE_INDEXABLE_NAMES.contains(fileName)) return true;
            int extensionStart = fileName.lastIndexOf('.');
            return extensionStart > 0 && WORKSPACE_INDEXABLE_EXTENSIONS.contains(fileName.substring(extensionStart + 1));
        } catch (ApiException exception) {
            return false;
        }
    }

    private boolean isUtf8Text(byte[] content) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content)).toString();
            for (int offset = 0; offset < text.length();) {
                int codePoint = text.codePointAt(offset);
                if (Character.isISOControl(codePoint) && codePoint != '\t' && codePoint != '\n'
                        && codePoint != '\r' && codePoint != '\f') return false;
                offset += Character.charCount(codePoint);
            }
            return true;
        } catch (java.nio.charset.CharacterCodingException exception) {
            return false;
        }
    }

    public WorkspaceFileContent readFile(AuthenticatedUser user, long workspaceId, String rawPath) {
        WorkspaceRecord workspace = accessibleWorkspace(user, workspaceId);
        Path file = safeFile(workspace, rawPath);
        try {
            String relativePath = normalizePath(rawPath);
            rejectReservedWorkspacePath(relativePath);
            byte[] content = readWorkspaceFile(safeRoot(workspace.storagePath()), relativePath,
                    config.maxFileBytes());
            WorkspaceFile metadata = repository.findFile(workspaceId, relativePath);
            if (metadata == null) throw new ApiException(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "文件不存在");
            return new WorkspaceFileContent(metadata, new String(content, StandardCharsets.UTF_8));
        } catch (SnapshotTooLargeException exception) {
            throw fileTooLarge();
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "文件不存在");
        }
    }

    @Transactional
    public WorkspaceFile writeFile(AuthenticatedUser user, long workspaceId, String rawPath,
                                   WorkspaceModels.WriteFileRequest request) {
        WorkspaceRecord workspace = accessibleWorkspace(user, workspaceId);
        repository.lockWorkspaceForMutation(workspaceId);
        return withWorkspacePaused(workspace,
                () -> writeWorkspaceFile(workspace, workspaceId, rawPath, request));
    }

    private WorkspaceFile writeWorkspaceFile(WorkspaceRecord workspace, long workspaceId, String rawPath,
                                              WorkspaceModels.WriteFileRequest request) {
        Path file = safeFile(workspace, rawPath);
        Path workspaceRoot = safeRoot(workspace.storagePath());
        String path = normalizePath(rawPath);
        rejectReservedWorkspacePath(path);
        WorkspaceFile previous = repository.findFile(workspaceId, path);
        if (request.expectedHash() != null) {
            String currentHash = null;
            try {
                currentHash = sha256(readWorkspaceFile(workspaceRoot, path, config.maxFileBytes()));
            } catch (IOException exception) {
                throw new ApiException(HttpStatus.CONFLICT, "FILE_HASH_MISMATCH", "文件已被修改，请刷新后重试");
            }
            if (currentHash == null || !request.expectedHash().equalsIgnoreCase(currentHash)) {
                throw new ApiException(HttpStatus.CONFLICT, "FILE_HASH_MISMATCH", "文件已被修改，请刷新后重试");
            }
        }
        byte[] content = request.content().getBytes(StandardCharsets.UTF_8);
        if (content.length > config.maxFileBytes()) throw fileTooLarge();
        if (requireSecureDirectoryStream) {
            byte[] previousContent;
            boolean existed;
            try {
                try {
                    previousContent = readWorkspaceFile(workspaceRoot, path, config.maxFileBytes());
                    existed = true;
                } catch (java.nio.file.NoSuchFileException missing) {
                    previousContent = null;
                    existed = false;
                }
                registerWorkspaceFileRollback(workspaceRoot, file, previousContent, existed, sha256(content));
                writeWorkspaceFileSecure(workspaceRoot, path, content, request.expectedHash());
                WorkspaceFile saved = repository.saveFile(workspaceId, path, displayName(path), language(path),
                        content.length, sha256(content), previous != null && previous.entry());
                return saved;
            } catch (ApiException exception) {
                throw exception;
            } catch (SnapshotTooLargeException exception) {
                throw fileTooLarge();
            } catch (IOException exception) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_WRITE_FAILED", "文件保存失败");
            }
        }
        Path temporary = file.resolveSibling("." + file.getFileName() + ".write-" + UUID.randomUUID());
        boolean existed = false;
        boolean replaced = false;
        byte[] previousContent = null;
        Set<PosixFilePermission> originalPermissions = null;
        try {
            Files.createDirectories(file.getParent());
            if (Files.exists(file, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(file)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_FILE_PATH", "不允许写入符号链接");
            }
            existed = Files.exists(file, LinkOption.NOFOLLOW_LINKS);
            if (existed) {
                previousContent = readBounded(file, config.maxFileBytes());
                try {
                    originalPermissions = Files.getPosixFilePermissions(file, LinkOption.NOFOLLOW_LINKS);
                } catch (UnsupportedOperationException ignored) {
                    // Non-POSIX test and development filesystems have no executable-bit metadata.
                }
            }
            Files.write(temporary, content);
            if (originalPermissions != null) Files.setPosixFilePermissions(temporary, originalPermissions);
            if (request.expectedHash() != null) {
                if (!existed || !request.expectedHash().equalsIgnoreCase(sha256(readBounded(file, config.maxFileBytes())))) {
                    throw new ApiException(HttpStatus.CONFLICT, "FILE_HASH_MISMATCH", "文件已被修改，请刷新后重试");
                }
            }
            moveReplace(temporary, file);
            replaced = true;
            WorkspaceFile saved = repository.saveFile(workspaceId, path, displayName(path), language(path),
                    content.length, sha256(content), previous != null && previous.entry());
            registerWorkspaceFileRollback(workspaceRoot, file, previousContent, existed, sha256(content));
            return saved;
        } catch (ApiException exception) {
            deleteSnapshotTree(temporary);
            if (replaced) restoreWorkspaceFile(workspaceRoot, file, previousContent, existed, sha256(content));
            throw exception;
        } catch (SnapshotTooLargeException exception) {
            deleteSnapshotTree(temporary);
            if (replaced) restoreWorkspaceFile(workspaceRoot, file, previousContent, existed, sha256(content));
            throw fileTooLarge();
        } catch (IOException exception) {
            deleteSnapshotTree(temporary);
            if (replaced) restoreWorkspaceFile(workspaceRoot, file, previousContent, existed, sha256(content));
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_WRITE_FAILED", "文件保存失败");
        } catch (RuntimeException exception) {
            deleteSnapshotTree(temporary);
            if (replaced) restoreWorkspaceFile(workspaceRoot, file, previousContent, existed, sha256(content));
            throw exception;
        }
    }

    private void writeWorkspaceFileSecure(Path workspaceRoot, String relativePath, byte[] content,
                                          String expectedHash) throws IOException {
        try (SecureWorkspaceParent parent = openSecureWorkspaceParent(workspaceRoot, relativePath)) {
            BasicFileAttributeView targetAttributes = parent.directory().getFileAttributeView(
                    parent.fileName(), BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
            boolean existed;
            Set<PosixFilePermission> originalPermissions = null;
            try {
                BasicFileAttributes attributes = targetAttributes.readAttributes();
                if (!attributes.isRegularFile() || attributes.isSymbolicLink() || attributes.fileKey() == null) {
                    throw new IOException("目标文件不是可安全替换的普通文件");
                }
                existed = true;
                PosixFileAttributeView posixAttributes = parent.directory().getFileAttributeView(
                        parent.fileName(), PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
                if (posixAttributes != null) originalPermissions = posixAttributes.readAttributes().permissions();
            } catch (java.nio.file.NoSuchFileException missing) {
                existed = false;
            }
            if (!existed && expectedHash != null) {
                throw new ApiException(HttpStatus.CONFLICT, "FILE_HASH_MISMATCH", "文件已被修改，请刷新后重试");
            }
            Path temporary = Path.of(".write-" + UUID.randomUUID());
            Path backup = Path.of(".write-backup-" + UUID.randomUUID());
            boolean temporaryExists = false;
            boolean backupExists = false;
            try {
                try (SeekableByteChannel channel = parent.directory().newByteChannel(temporary,
                        Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS))) {
                    temporaryExists = true;
                    Channels.newOutputStream(channel).write(content);
                }
                if (originalPermissions != null) {
                    PosixFileAttributeView temporaryAttributes = parent.directory().getFileAttributeView(
                            temporary, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
                    if (temporaryAttributes == null) throw new IOException("无法保留工作区文件权限");
                    temporaryAttributes.setPermissions(originalPermissions);
                }
                if (existed) {
                    parent.directory().move(parent.fileName(), parent.directory(), backup);
                    backupExists = true;
                    if (expectedHash != null) {
                        String movedHash;
                        try (SeekableByteChannel channel = parent.directory().newByteChannel(backup,
                                Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS))) {
                            movedHash = sha256(readBounded(Channels.newInputStream(channel), config.maxFileBytes()));
                        }
                        if (!expectedHash.equalsIgnoreCase(movedHash)) {
                            parent.directory().move(backup, parent.directory(), parent.fileName());
                            backupExists = false;
                            throw new ApiException(HttpStatus.CONFLICT, "FILE_HASH_MISMATCH",
                                    "文件已被修改，请刷新后重试");
                        }
                    }
                }
                parent.directory().move(temporary, parent.directory(), parent.fileName());
                temporaryExists = false;
                if (backupExists) {
                    parent.directory().deleteFile(backup);
                    backupExists = false;
                }
            } catch (IOException | RuntimeException exception) {
                if (backupExists) {
                    try {
                        parent.directory().move(backup, parent.directory(), parent.fileName());
                        backupExists = false;
                    } catch (IOException | RuntimeException restoreFailure) {
                        exception.addSuppressed(restoreFailure);
                    }
                }
                throw exception;
            } finally {
                if (temporaryExists) {
                    try { parent.directory().deleteFile(temporary); }
                    catch (java.nio.file.NoSuchFileException ignored) { }
                }
                if (backupExists) {
                    log.error("Secure workspace write left a recoverable backup file '{}'", backup);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T withWorkspacePaused(WorkspaceRecord workspace, Supplier<T> operation) {
        String runtimeInstanceId = workspace.runtimeInstanceId();
        if (runtimeInstanceId == null || runtimeInstanceId.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "WORKSPACE_RUNTIME_MISSING",
                    "工作区容器不可用，已阻止文件操作");
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            Set<String> pausedRuntimes = (Set<String>) TransactionSynchronizationManager
                    .getResource(pausedRuntimeResourceKey);
            if (pausedRuntimes == null) {
                pausedRuntimes = new HashSet<>();
                TransactionSynchronizationManager.bindResource(pausedRuntimeResourceKey, pausedRuntimes);
            }
            boolean newlyPaused = pausedRuntimes.add(runtimeInstanceId);
            if (newlyPaused) {
                try {
                    runtime.pause(runtimeInstanceId);
                } catch (RuntimeException exception) {
                    pausedRuntimes.remove(runtimeInstanceId);
                    if (pausedRuntimes.isEmpty()) {
                        TransactionSynchronizationManager.unbindResource(pausedRuntimeResourceKey);
                    }
                    log.warn("Could not pause workspace '{}' before a filesystem operation: {}",
                            workspace.id(), exception.toString());
                    throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "WORKSPACE_QUIESCE_FAILED",
                            "工作区仍有进程运行，暂时无法安全保存或快照");
                }
                Set<String> runtimesToResume = pausedRuntimes;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public int getOrder() {
                        return Ordered.LOWEST_PRECEDENCE;
                    }

                    @Override
                    public void afterCompletion(int status) {
                        try {
                            for (String instanceId : runtimesToResume) {
                                try {
                                    runtime.resume(instanceId);
                                } catch (RuntimeException exception) {
                                    log.error("Could not resume workspace container '{}' after filesystem operation",
                                            instanceId, exception);
                                }
                            }
                        } finally {
                            if (TransactionSynchronizationManager.hasResource(pausedRuntimeResourceKey)) {
                                TransactionSynchronizationManager.unbindResource(pausedRuntimeResourceKey);
                            }
                        }
                    }
                });
            }
            return operation.get();
        }

        try {
            runtime.pause(runtimeInstanceId);
        } catch (RuntimeException exception) {
            log.warn("Could not pause workspace '{}' before a filesystem operation: {}",
                    workspace.id(), exception.toString());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "WORKSPACE_QUIESCE_FAILED",
                    "工作区仍有进程运行，暂时无法安全保存或快照");
        }
        RuntimeException operationFailure = null;
        try {
            return operation.get();
        } catch (RuntimeException exception) {
            operationFailure = exception;
            throw exception;
        } finally {
            try {
                runtime.resume(runtimeInstanceId);
            } catch (RuntimeException resumeFailure) {
                if (operationFailure != null) {
                    operationFailure.addSuppressed(resumeFailure);
                } else {
                    log.error("Could not resume workspace '{}' after a filesystem operation", workspace.id(), resumeFailure);
                    throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "WORKSPACE_RESUME_FAILED",
                            "工作区恢复失败，请联系教师或管理员");
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void registerWorkspaceFileRollback(Path workspaceRoot, Path file, byte[] originalContent,
                                               boolean existed, String expectedCurrentHash) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        Path canonicalFile;
        try {
            Path realRoot = workspaceRoot.toRealPath();
            canonicalFile = file.getParent().toRealPath().resolve(file.getFileName()).normalize();
            if (!canonicalFile.startsWith(realRoot)) throw new IOException("回滚路径越出工作区");
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_FILE_PATH", "工作区文件路径无效");
        }
        Map<Path, WorkspaceFileBeforeImage> beforeImages =
                (Map<Path, WorkspaceFileBeforeImage>) TransactionSynchronizationManager.getResource(this);
        if (beforeImages == null) {
            beforeImages = new LinkedHashMap<>();
            TransactionSynchronizationManager.bindResource(this, beforeImages);
            Map<Path, WorkspaceFileBeforeImage> registeredBeforeImages = beforeImages;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public int getOrder() {
                    return Ordered.HIGHEST_PRECEDENCE;
                }

                @Override
                public void afterCompletion(int status) {
                    try {
                        if (status != STATUS_COMMITTED) {
                            registeredBeforeImages.forEach((path, beforeImage) -> {
                                try {
                                    restoreWorkspaceFile(beforeImage.workspaceRoot(), path,
                                            beforeImage.content(), beforeImage.existed(), beforeImage.expectedCurrentHash());
                                } catch (RuntimeException exception) {
                                    log.error("Workspace transaction rollback failed for file '{}'",
                                            path.getFileName(), exception);
                                }
                            });
                        }
                    } finally {
                        if (TransactionSynchronizationManager.hasResource(WorkspaceService.this)) {
                            TransactionSynchronizationManager.unbindResource(WorkspaceService.this);
                        }
                    }
                }
            });
        }
        WorkspaceFileBeforeImage beforeImage = beforeImages.get(canonicalFile);
        if (beforeImage == null) {
            beforeImages.put(canonicalFile, new WorkspaceFileBeforeImage(workspaceRoot,
                    originalContent == null ? null : originalContent.clone(), existed, expectedCurrentHash));
        } else {
            beforeImages.put(canonicalFile, new WorkspaceFileBeforeImage(beforeImage.workspaceRoot(),
                    beforeImage.content(), beforeImage.existed(), expectedCurrentHash));
        }
    }

    private void moveReplace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void restoreWorkspaceFile(Path workspaceRoot, Path file, byte[] originalContent, boolean existed,
                                      String expectedCurrentHash) {
        if (requireSecureDirectoryStream) {
            restoreWorkspaceFileSecure(workspaceRoot, file, originalContent, existed, expectedCurrentHash);
            return;
        }
        Path temporary = file.resolveSibling("." + file.getFileName() + ".restore-" + UUID.randomUUID());
        try {
            rejectSymlinkComponents(workspaceRoot, file);
            if (!file.getParent().toRealPath().startsWith(workspaceRoot.toRealPath())) {
                throw new IOException("回滚路径越出工作区");
            }
            if (expectedCurrentHash == null || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                    || !expectedCurrentHash.equals(sha256(readBounded(file, config.maxFileBytes())))) {
                log.warn("Workspace rollback left externally modified file '{}' untouched", file.getFileName());
                return;
            }
            if (!existed) {
                Files.delete(file);
                return;
            }
            Set<PosixFilePermission> originalPermissions = null;
            try {
                originalPermissions = Files.getPosixFilePermissions(file, LinkOption.NOFOLLOW_LINKS);
            } catch (UnsupportedOperationException ignored) {
                // Non-POSIX filesystems have no executable-bit metadata.
            } catch (java.nio.file.NoSuchFileException ignored) {
                // The file disappeared while the transaction was rolling back.
            }
            Files.write(temporary, originalContent == null ? new byte[0] : originalContent,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
            if (originalPermissions != null) Files.setPosixFilePermissions(temporary, originalPermissions);
            moveReplace(temporary, file);
        } catch (IOException ignored) {
            deleteSnapshotTree(temporary);
            log.error("Workspace file rollback could not restore file '{}'; manual recovery may be required",
                    file.getFileName(), ignored);
        }
    }

    private void restoreWorkspaceFileSecure(Path workspaceRoot, Path file, byte[] originalContent, boolean existed,
                                            String expectedCurrentHash) {
        List<SecureDirectoryStream<Path>> openedDirectories = new java.util.ArrayList<>();
        Path temporaryName = Path.of(".restore-" + UUID.randomUUID());
        Path backupName = Path.of(".restore-backup-" + UUID.randomUUID());
        SecureDirectoryStream<Path> recoveryDirectory = null;
        boolean temporaryCreated = false;
        boolean backupCreated = false;
        try {
            BasicFileAttributes expectedRoot = Files.readAttributes(
                    workspaceRoot, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!expectedRoot.isDirectory() || expectedRoot.isSymbolicLink()) {
                throw new IOException("工作区根目录无效");
            }
            DirectoryStream<Path> rootDirectory = Files.newDirectoryStream(workspaceRoot);
            if (!(rootDirectory instanceof SecureDirectoryStream<?> secureDirectory)) {
                rootDirectory.close();
                throw new IOException("安全文件恢复需要 SecureDirectoryStream 支持");
            }
            @SuppressWarnings("unchecked")
            SecureDirectoryStream<Path> current = (SecureDirectoryStream<Path>) secureDirectory;
            openedDirectories.add(current);
            BasicFileAttributes openedRoot = current.getFileAttributeView(BasicFileAttributeView.class)
                    .readAttributes();
            if (expectedRoot.fileKey() == null || !expectedRoot.fileKey().equals(openedRoot.fileKey())) {
                throw new IOException("工作区根目录在恢复期间发生变化");
            }
            Path realRoot = workspaceRoot.toRealPath();
            Path relative = realRoot.relativize(file.toAbsolutePath().normalize());
            if (relative.isAbsolute() || relative.getNameCount() == 0 || relative.startsWith("..")) {
                throw new IOException("回滚路径越出工作区");
            }
            for (int index = 0; index < relative.getNameCount() - 1; index++) {
                current = current.newDirectoryStream(relative.getName(index), LinkOption.NOFOLLOW_LINKS);
                openedDirectories.add(current);
            }
            recoveryDirectory = current;
            Path fileName = relative.getFileName();
            try {
                current.move(fileName, current, backupName);
                backupCreated = true;
            } catch (java.nio.file.NoSuchFileException missing) {
                log.warn("Workspace rollback left externally removed file '{}' untouched", file.getFileName());
                return;
            }
            String currentHash;
            try (SeekableByteChannel channel = current.newByteChannel(backupName,
                    Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS))) {
                currentHash = sha256(readBounded(Channels.newInputStream(channel), config.maxFileBytes()));
            }
            if (expectedCurrentHash == null || !expectedCurrentHash.equals(currentHash)) {
                current.move(backupName, current, fileName);
                backupCreated = false;
                log.warn("Workspace rollback left externally modified file '{}' untouched", file.getFileName());
                return;
            }
            if (!existed) {
                current.deleteFile(backupName);
                backupCreated = false;
                return;
            }
            Set<PosixFilePermission> originalPermissions = null;
            PosixFileAttributeView existingPosix = current.getFileAttributeView(
                    backupName, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
            if (existingPosix != null) {
                try {
                    originalPermissions = existingPosix.readAttributes().permissions();
                } catch (java.nio.file.NoSuchFileException ignored) {
                    // The file disappeared while the transaction was rolling back.
                }
            }
            try (SeekableByteChannel channel = current.newByteChannel(temporaryName,
                    Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS))) {
                temporaryCreated = true;
                Channels.newOutputStream(channel).write(originalContent == null ? new byte[0] : originalContent);
            }
            if (originalPermissions != null) {
                PosixFileAttributeView temporaryPosix = current.getFileAttributeView(
                        temporaryName, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
                if (temporaryPosix == null) throw new IOException("无法保留回滚文件权限");
                temporaryPosix.setPermissions(originalPermissions);
            }
            current.move(temporaryName, current, fileName);
            temporaryCreated = false;
            if (backupCreated) {
                current.deleteFile(backupName);
                backupCreated = false;
            }
        } catch (IOException | RuntimeException exception) {
            if (backupCreated && recoveryDirectory != null) {
                try {
                    recoveryDirectory.move(backupName, recoveryDirectory, file.getFileName());
                    backupCreated = false;
                } catch (IOException | RuntimeException recoveryFailure) {
                    exception.addSuppressed(recoveryFailure);
                }
            }
            log.error("Workspace transaction rollback could not safely restore file '{}'",
                    file.getFileName(), exception);
        } finally {
            if (temporaryCreated && recoveryDirectory != null) {
                try {
                    recoveryDirectory.deleteFile(temporaryName);
                } catch (java.nio.file.NoSuchFileException ignored) {
                    // The incomplete temporary file is already absent.
                } catch (IOException | RuntimeException cleanupFailure) {
                    log.error("Workspace rollback left an incomplete recovery file '{}'",
                            temporaryName, cleanupFailure);
                }
            }
            closeSecureDirectories(openedDirectories);
        }
    }

    @Transactional
    public void applyPatch(AuthenticatedUser user, long workspaceId,
                                         List<PatchModels.PatchFile> files) {
        WorkspaceRecord workspace = accessibleWorkspace(user, workspaceId);
        repository.lockWorkspaceForMutation(workspaceId);
        withWorkspacePaused(workspace, () -> {
            applyPatchWhilePaused(user, workspaceId, files);
            return null;
        });
    }

    private void applyPatchWhilePaused(AuthenticatedUser user, long workspaceId,
                                       List<PatchModels.PatchFile> files) {
        for (PatchModels.PatchFile patch : files) {
            if (patch.content() == null) throw new ApiException(HttpStatus.BAD_REQUEST, "PATCH_CONTENT_REQUIRED", "补丁内容不能为空");
            if (patch.content().getBytes(StandardCharsets.UTF_8).length > config.maxFileBytes()) throw fileTooLarge();
            WorkspaceFileContent current = readFile(user, workspaceId, patch.path());
            if (!patch.expectedHash().equalsIgnoreCase(sha256(current.content().getBytes(StandardCharsets.UTF_8)))) {
                throw new ApiException(HttpStatus.CONFLICT, "FILE_HASH_MISMATCH", "文件已被修改，请刷新后重试");
            }
        }
        for (PatchModels.PatchFile patch : files) {
            writeFile(user, workspaceId, patch.path(),
                    new WorkspaceModels.WriteFileRequest(patch.content(), patch.expectedHash()));
        }
    }

    @Transactional
    public SnapshotView createSnapshot(AuthenticatedUser user, long workspaceId,
                                       WorkspaceModels.SnapshotRequest request) {
        WorkspaceRecord workspace = accessibleWorkspace(user, workspaceId);
        repository.lockWorkspaceForMutation(workspaceId);
        return withWorkspacePaused(workspace, () -> createSnapshotWhilePaused(workspace, workspaceId, request));
    }

    private SnapshotView createSnapshotWhilePaused(WorkspaceRecord workspace, long workspaceId,
                                                   WorkspaceModels.SnapshotRequest request) {
        Path root = safeRoot(workspace.storagePath());
        String snapshotId = UUID.randomUUID().toString();
        String storageKey = ".manual-snapshots/" + snapshotId;
        Path snapshotParent = root.resolveSibling(root.getFileName() + ".manual-snapshots").normalize();
        Path snapshotRoot = snapshotParent.resolve(snapshotId).normalize();
        Path configuredRoot = Path.of(config.rootPath()).toAbsolutePath().normalize();
        if (!snapshotParent.startsWith(configuredRoot) || !snapshotRoot.startsWith(snapshotParent)) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SNAPSHOT_PATH_INVALID", "快照路径无效");
        }
        try {
            rejectSymlinkComponents(root.getRoot(), root);
            rejectSymlinkComponents(root.getRoot(), snapshotParent);
            Files.createDirectories(snapshotParent);
            Files.createDirectory(snapshotRoot);
            copyWorkspaceEvidence(root, snapshotRoot);
            SnapshotView snapshot = repository.createSnapshot(workspaceId, storageKey,
                    request == null ? null : request.description());
            registerSnapshotRollbackCleanup(snapshotRoot);
            return snapshot;
        } catch (SnapshotTooLargeException exception) {
            deleteSnapshotTree(snapshotRoot);
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "SNAPSHOT_TOO_LARGE", "快照超过大小限制");
        } catch (UnsupportedSnapshotTextException exception) {
            deleteSnapshotTree(snapshotRoot);
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "SNAPSHOT_FILE_UNSUPPORTED", "快照仅支持有效 UTF-8 文本文件");
        } catch (IOException | ArithmeticException exception) {
            deleteSnapshotTree(snapshotRoot);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SNAPSHOT_CREATE_FAILED", "工作区快照创建失败");
        } catch (RuntimeException exception) {
            deleteSnapshotTree(snapshotRoot);
            throw exception;
        }
    }

    private WorkspaceRecord accessibleWorkspace(AuthenticatedUser user, long workspaceId) {
        requireStudent(user);
        WorkspaceRecord workspace = repository.findWorkspace(workspaceId, user.id());
        if (!"running".equals(workspace.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "WORKSPACE_NOT_READY", "工作区尚未就绪");
        }
        return workspace;
    }

    private Path safeRoot(String rawPath) {
        Path configured = Path.of(config.rootPath()).toAbsolutePath().normalize();
        Path root = Path.of(rawPath).toAbsolutePath().normalize();
        if (!root.startsWith(configured)) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_WORKSPACE_PATH", "工作区路径无效");
        return root;
    }

    private Path safeFile(WorkspaceRecord workspace, String rawPath) {
        String path = normalizePath(rawPath);
        Path root = safeRoot(workspace.storagePath());
        Path file = root.resolve(path).normalize();
        if (!file.startsWith(root) || path.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_FILE_PATH", "文件路径无效");
        }
        try {
            Path realRoot = root.toRealPath();
            Path existingAncestor = file;
            while (existingAncestor != null && !Files.exists(existingAncestor, LinkOption.NOFOLLOW_LINKS)) {
                existingAncestor = existingAncestor.getParent();
            }
            if (existingAncestor == null) throw new IOException("路径不存在");
            Path realParent = existingAncestor.toRealPath();
            if (!realParent.startsWith(realRoot)) throw new IOException("路径越界");
            if (Files.exists(file, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(file)) {
                throw new IOException("不允许访问符号链接");
            }
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_FILE_PATH", "文件路径无效");
        }
        return file;
    }

    static String normalizePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank() || rawPath.indexOf('\0') >= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_FILE_PATH", "文件路径不能为空或包含无效字符");
        }
        String path = rawPath.replace('\\', '/');
        if (path.startsWith("/") || path.matches("^[A-Za-z]:.*") || path.contains("../")
                || path.equals("..") || path.contains("/..")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_FILE_PATH", "不允许访问工作区之外的文件");
        }
        try {
            String normalized = Path.of(path).normalize().toString().replace('\\', '/');
            if (normalized.isBlank() || normalized.equals(".")) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_FILE_PATH", "文件路径不能为空");
            }
            return normalized;
        } catch (java.nio.file.InvalidPathException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_FILE_PATH", "文件路径格式无效");
        }
    }

    private void rejectReservedWorkspacePath(String path) {
        String normalized = path.toLowerCase(Locale.ROOT);
        if (normalized.equals(".snapshots") || normalized.startsWith(".snapshots/")
                || normalized.equals(".node-evidence") || normalized.startsWith(".node-evidence/")
                || normalized.equals(".manual-snapshots") || normalized.startsWith(".manual-snapshots/")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_FILE_PATH", "该路径属于只读快照存储区");
        }
    }

    private String displayName(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }

    private String language(String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? null : path.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private ApiException fileTooLarge() {
        return new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "文件超过大小限制");
    }

    private void deleteSnapshotTree(Path snapshotRoot) {
        Throwable failure = null;
        try {
            Files.readAttributes(snapshotRoot, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (java.nio.file.NoSuchFileException missing) {
            return;
        } catch (IOException | SecurityException inspectionFailure) {
            log.error("Snapshot cleanup could not inspect target {}; retryable orphan cleanup is required",
                    snapshotRoot.getFileName(), inspectionFailure);
            return;
        }
        try (Stream<Path> paths = Files.walk(snapshotRoot)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException | SecurityException exception) {
                    log.error("Snapshot cleanup could not remove {}; retryable orphan cleanup is required",
                            path.getFileName(), exception);
                }
            });
        } catch (IOException | java.io.UncheckedIOException | SecurityException exception) {
            failure = exception;
        }
        if (failure != null) {
            log.error("Snapshot cleanup failed; retryable orphan cleanup is required at {}",
                    snapshotRoot.getFileName(), failure);
        }
    }

    private record WorkspaceFileBeforeImage(Path workspaceRoot, byte[] content, boolean existed,
                                            String expectedCurrentHash) {}

    private record IndexedWorkspaceFile(String path, long sizeBytes, String contentHash) {}

    private static final class WorkspaceIndexLimitException extends IOException {}

    private static final class SnapshotTooLargeException extends IOException {}

    private static final class UnsupportedSnapshotTextException extends IOException {
        private UnsupportedSnapshotTextException(String message) { super(message); }
        private UnsupportedSnapshotTextException(String message, Throwable cause) { super(message, cause); }
    }

    private void requireStudent(AuthenticatedUser user) {
        if (!"student".equals(user.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ROLE_FORBIDDEN", "只有学生可以操作实验工作区");
        }
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    public static String sha256Text(String content) {
        if (content == null) throw new IllegalArgumentException("内容不能为空");
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }
}
