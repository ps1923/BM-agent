package com.bmhs.workspace;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.experimentcreation.ApiException;
import com.bmhs.workspace.TerminalModels.CommandRequest;
import com.bmhs.workspace.TerminalModels.CommandView;
import com.bmhs.workspace.TerminalModels.TerminalSessionView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;

@Service
public class TerminalService {
    private static final int MAX_CONFIGURED_OUTPUT_BYTES = 1_048_576;
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

    public TerminalSessionView createSession(AuthenticatedUser user, long workspaceId) {
        requireStudent(user);
        workspaceRepository.findWorkspace(workspaceId, user.id());
        return repository.createSession(workspaceId, user.id());
    }

    @Transactional
    public CommandView execute(AuthenticatedUser user, long sessionId, CommandRequest request) {
        requireStudent(user);
        TerminalRepository.SessionRecord session = repository.findSession(sessionId, user.id());
        WorkspaceModels.WorkspaceRecord workspace = workspaceRepository.findWorkspace(session.workspaceId(), user.id());
        workspaceRepository.lockWorkspaceForMutation(workspace.id());
        List<String> command = CommandPolicy.parse(request.commandText());
        String workingDirectory = CommandPolicy.workingDirectory(request.workingDirectory());
        long commandId = repository.createCommand(sessionId, String.join(" ", command), workingDirectory);
        TerminalRuntime.TerminalResult result;
        try {
            result = runtime.execute(workspace, command, workingDirectory,
                    commandTimeout.toMillis(), maxOutputBytes);
        } catch (RuntimeException exception) {
            result = new TerminalRuntime.TerminalResult("failed", null, "终端执行失败");
        }
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
        Path log = root.resolve(".terminal").resolve("command-" + commandId + ".log").normalize();
        if (!log.startsWith(root)) throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "OUTPUT_STORAGE_FAILED", "命令输出路径无效");
        try {
            Files.createDirectories(log.getParent());
            Files.writeString(log, output == null ? "" : output, StandardCharsets.UTF_8);
            return ".terminal/" + log.getFileName();
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "OUTPUT_STORAGE_FAILED", "命令输出保存失败");
        }
    }

    private void requireStudent(AuthenticatedUser user) {
        if (!"student".equals(user.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ROLE_FORBIDDEN", "只有学生可以操作终端");
        }
    }
}
