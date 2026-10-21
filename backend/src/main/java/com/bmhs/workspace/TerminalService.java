package com.bmhs.workspace;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.experimentcreation.ApiException;
import com.bmhs.workspace.TerminalModels.CommandRequest;
import com.bmhs.workspace.TerminalModels.CommandRecord;
import com.bmhs.workspace.TerminalModels.CommandView;
import com.bmhs.workspace.TerminalModels.TerminalSessionView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.InvalidPathException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TerminalService {
    private static final int MAX_CONFIGURED_OUTPUT_BYTES = 1_048_576;
    private static final String UUID_PATTERN = "(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";
    private static final Set<String> TERMINAL_STATUSES = Set.of("succeeded", "failed", "cancelled");
    private final TerminalRepository repository;
    private final WorkspaceRepository workspaceRepository;
    private final TerminalRuntime runtime;
    private final Duration commandTimeout;
    private final int maxOutputBytes;

    public TerminalService(TerminalRepository repository, WorkspaceRepository workspaceRepository,
                           TerminalRuntime runtime,
                           @Value("${bm-hs.workspace.command-timeout:PT30S}") Duration commandTimeout,
                           @Value("${bm-hs.workspace.max-output-bytes:65536}") int maxOutputBytes) {
        if (commandTimeout.isZero() || commandTimeout.isNegative()
                || maxOutputBytes <= 0 || maxOutputBytes > MAX_CONFIGURED_OUTPUT_BYTES) {
            throw new IllegalArgumentException("终端限制配置无效");
        }
        this.repository = repository;
        this.workspaceRepository = workspaceRepository;
        this.runtime = runtime;
        this.commandTimeout = commandTimeout;
        this.maxOutputBytes = maxOutputBytes;
    }

    public TerminalSessionView createSession(AuthenticatedUser user, long workspaceId, String rawClientSessionId) {
        String clientSessionId = validateIdempotencyKey(rawClientSessionId);
        requireStudent(user);
        WorkspaceModels.WorkspaceRecord workspace = workspaceRepository.findWorkspace(workspaceId, user.id());
        requireServerWorkspace(workspace);
        return repository.createSession(workspaceId, user.id(), clientSessionId);
    }

    @Transactional
    public CommandView execute(AuthenticatedUser user, long sessionId, String rawClientCommandId,
                              CommandRequest request) {
        String clientCommandId = validateClientCommandId(rawClientCommandId);
        requireStudent(user);
        TerminalRepository.SessionRecord session = repository.findSession(sessionId, user.id());
        WorkspaceModels.WorkspaceRecord workspace = workspaceRepository.findWorkspace(session.workspaceId(), user.id());
        requireServerWorkspace(workspace);
        workspaceRepository.lockWorkspaceForMutation(workspace.id());
        CommandRecord existing = repository.findCommandForClaim(sessionId, clientCommandId, user.id());
        if (existing != null) {
            String requestedDirectory;
            try {
                requestedDirectory = CommandPolicy.workingDirectory(request.workingDirectory());
            } catch (ApiException invalidDirectory) {
                throw reusedIdempotencyKey();
            }
            if (!existing.commandText().equals(request.commandText())
                    || !existing.workingDirectory().equals(requestedDirectory)) {
                throw reusedIdempotencyKey();
            }
            return commandView(existing, readStoredOutput(existing));
        }
        if (!"open".equals(session.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "TERMINAL_SESSION_CLOSED", "终端会话已关闭");
        }
        List<String> command = CommandPolicy.parse(request.commandText());
        String workingDirectory = CommandPolicy.workingDirectory(request.workingDirectory());
        TerminalRepository.CommandClaim claim = repository.claimCommand(sessionId, clientCommandId,
                request.commandText(), workingDirectory);
        if (!claim.newlyClaimed()) {
            return commandView(claim.command(), readStoredOutput(claim.command()));
        }
        long commandId = claim.command().id();
        if (repository.markCommandRunning(commandId) != 1) {
            throw new IllegalStateException("终端命令状态无法安全转换为运行中");
        }
        TerminalRuntime.TerminalResult result = runtime.execute(workspace, command, workingDirectory,
                commandTimeout.toMillis(), maxOutputBytes);
        String status = normalizedStatus(result);
        String output = boundedOutput(result.output());
        String storageKey;
        try {
            storageKey = storeOutput(workspace, commandId, output);
        } catch (RuntimeException exception) {
            repository.finishCommand(commandId, "failed", null, null);
            throw exception;
        }
        repository.finishCommand(commandId, status,
                "succeeded".equals(status) ? Integer.valueOf(0) : result.exitCode(), storageKey);
        return repository.command(commandId, user.id(), output);
    }

    private void requireServerWorkspace(WorkspaceModels.WorkspaceRecord workspace) {
        if (!"server".equals(workspace.workspaceType())) {
            throw new ApiException(HttpStatus.CONFLICT, "DESKTOP_WORKSPACE_IS_LOCAL",
                    "本地桌面工作区不能通过服务器终端执行命令");
        }
    }

    public CommandView reconcile(AuthenticatedUser user, long sessionId, String rawClientCommandId) {
        String clientCommandId = validateClientCommandId(rawClientCommandId);
        requireStudent(user);
        CommandRecord command = repository.findCommand(sessionId, clientCommandId, user.id());
        return commandView(command, readStoredOutput(command));
    }

    private String validateClientCommandId(String value) {
        return validateIdempotencyKey(value);
    }

    private String validateIdempotencyKey(String value) {
        if (value == null || !value.matches(UUID_PATTERN)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY", "终端请求幂等键必须是 UUID");
        }
        return value.toLowerCase(java.util.Locale.ROOT);
    }

    private ApiException reusedIdempotencyKey() {
        return new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED", "幂等键已用于不同的命令内容");
    }

    private CommandView commandView(CommandRecord command, String output) {
        return new CommandView(command.id(), command.terminalSessionId(), command.commandText(), command.status(),
                command.exitCode(), output, command.createdAt());
    }

    private String readStoredOutput(CommandRecord command) {
        if (command.outputStorageKey() == null || command.outputStorageKey().isBlank()) return "";
        try {
            Path root = Path.of(command.storagePath()).toAbsolutePath().normalize();
            if (command.id() <= 0
                    || !("command-" + command.id() + ".log").equals(command.outputStorageKey())) return "";
            Path terminalDirectory = outputDirectory(root);
            Path output = terminalDirectory.resolve("command-" + command.id() + ".log");
            if (!Files.isDirectory(terminalDirectory, LinkOption.NOFOLLOW_LINKS)
                    || Files.isSymbolicLink(terminalDirectory)
                    || !Files.isRegularFile(output, LinkOption.NOFOLLOW_LINKS)
                    || Files.isSymbolicLink(output)) return "";
            Set<OpenOption> options = new HashSet<>();
            options.add(StandardOpenOption.READ);
            options.add(LinkOption.NOFOLLOW_LINKS);
            try (SeekableByteChannel channel = Files.newByteChannel(output, options)) {
                ByteBuffer buffer = ByteBuffer.allocate(maxOutputBytes + 64);
                while (buffer.hasRemaining() && channel.read(buffer) >= 0) { }
                buffer.flip();
                return StandardCharsets.UTF_8.decode(buffer).toString();
            }
        } catch (IOException | InvalidPathException exception) {
            return "";
        }
    }

    private String normalizedStatus(TerminalRuntime.TerminalResult result) {
        if (result == null || !TERMINAL_STATUSES.contains(result.status())) return "failed";
        if ("succeeded".equals(result.status()) && !Integer.valueOf(0).equals(result.exitCode())) return "failed";
        return result.status();
    }

    String boundedOutput(String output) {
        if (output == null || output.isEmpty()) return "";
        StringBuilder bounded = new StringBuilder();
        int used = 0;
        for (int offset = 0; offset < output.length();) {
            int codePoint = output.codePointAt(offset);
            int partBytes = utf8Length(codePoint);
            if (used + partBytes > maxOutputBytes) {
                return bounded + "\n[输出已截断]";
            }
            bounded.appendCodePoint(codePoint);
            used += partBytes;
            offset += Character.charCount(codePoint);
        }
        return bounded.toString();
    }

    private int utf8Length(int codePoint) {
        if (codePoint <= 0x7f || (codePoint >= Character.MIN_SURROGATE && codePoint <= Character.MAX_SURROGATE)) return 1;
        if (codePoint <= 0x7ff) return 2;
        if (codePoint <= 0xffff) return 3;
        return 4;
    }

    private String storeOutput(WorkspaceModels.WorkspaceRecord workspace, long commandId, String output) {
        Path root = Path.of(workspace.storagePath()).toAbsolutePath().normalize();
        Path terminalDirectory = outputDirectory(root);
        Path log = terminalDirectory.resolve("command-" + commandId + ".log");
        if (!log.startsWith(terminalDirectory) || commandId <= 0) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "OUTPUT_STORAGE_FAILED", "命令输出路径无效");
        }
        try {
            if (Files.notExists(terminalDirectory, LinkOption.NOFOLLOW_LINKS)) {
                Files.createDirectory(terminalDirectory);
            }
            if (!Files.isDirectory(terminalDirectory, LinkOption.NOFOLLOW_LINKS)
                    || Files.isSymbolicLink(terminalDirectory)) {
                throw new IOException("终端输出目录不是安全目录");
            }
            Set<OpenOption> options = new HashSet<>();
            options.add(StandardOpenOption.CREATE_NEW);
            options.add(StandardOpenOption.WRITE);
            options.add(LinkOption.NOFOLLOW_LINKS);
            try (SeekableByteChannel channel = Files.newByteChannel(log, options)) {
                ByteBuffer data = StandardCharsets.UTF_8.encode(output == null ? "" : output);
                while (data.hasRemaining()) channel.write(data);
            }
            return "command-" + commandId + ".log";
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "OUTPUT_STORAGE_FAILED", "命令输出保存失败");
        }
    }

    private Path outputDirectory(Path workspaceRoot) {
        Path name = workspaceRoot.getFileName();
        if (name == null || workspaceRoot.getParent() == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "OUTPUT_STORAGE_FAILED", "命令输出路径无效");
        }
        return workspaceRoot.resolveSibling(name + ".terminal-logs").normalize();
    }

    private void requireStudent(AuthenticatedUser user) {
        if (!"student".equals(user.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ROLE_FORBIDDEN", "只有学生可以操作终端");
        }
    }
}
