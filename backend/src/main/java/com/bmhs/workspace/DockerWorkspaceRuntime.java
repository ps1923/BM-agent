package com.bmhs.workspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class DockerWorkspaceRuntime implements WorkspaceRuntime {
    private static final Logger log = LoggerFactory.getLogger(DockerWorkspaceRuntime.class);
    private static final int MAX_CLI_OUTPUT_BYTES = 16_384;
    private static final int CLI_OUTPUT_READ_BUFFER_BYTES = 4_096;
    private static final Duration PROCESS_KILL_GRACE = Duration.ofSeconds(1);
    private static final Duration CONTAINER_CONTROL_TIMEOUT = Duration.ofSeconds(5);
    private final String dockerBinary;
    private final Duration startTimeout;

    public DockerWorkspaceRuntime(
            @Value("${bm-hs.workspace.docker-binary:docker}") String dockerBinary,
            @Value("${bm-hs.workspace.start-timeout:PT30S}") Duration startTimeout) {
        if (startTimeout == null || startTimeout.isZero() || startTimeout.isNegative()) {
            throw new IllegalArgumentException("Docker 工作区启动超时必须大于 0");
        }
        this.dockerBinary = dockerBinary;
        this.startTimeout = startTimeout;
    }

    @Override
    public String start(WorkspaceModels.WorkspaceRecord workspace) {
        String containerName = "bm-hs-run-" + workspace.runId();
        try {
            List<String> command = new ArrayList<>(List.of(
                    dockerBinary, "run", "-d", "--name", containerName,
                    "--network", "none", "--cap-drop", "ALL",
                    "--security-opt", "no-new-privileges", "--pids-limit", "128",
                    "--user", containerUser(workspace),
                    "--read-only", "--workdir", "/workspace",
                    "--cpus", workspace.cpuLimit().toPlainString(),
                    "--memory", workspace.memoryLimitMb() + "m",
                    "--tmpfs", "/tmp:rw,noexec,nosuid,size=64m",
                    "-v", workspace.storagePath() + ":/workspace:rw",
                    workspace.runtimeImage(), "sleep", "infinity"));
            ProcessResult result = runCommand(command, startTimeout);
            if (result.exitCode() != 0 || result.outputTruncated()) {
                throw new IllegalStateException("Docker 工作区启动失败");
            }
            return parseContainerId(result.output());
        } catch (IOException exception) {
            cleanupAfterFailure(containerName, exception);
            throw new IllegalStateException("Docker 运行时不可用", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            cleanupAfterFailure(containerName, exception);
            throw new IllegalStateException("Docker 工作区启动被中断", exception);
        } catch (RuntimeException exception) {
            cleanupAfterFailure(containerName, exception);
            throw exception;
        }
    }

    @Override
    public void stop(String runtimeInstanceId) {
        if (runtimeInstanceId == null || !runtimeInstanceId.matches("[0-9a-f]{12,64}")) {
            throw new IllegalArgumentException("Docker 工作区实例 ID 格式无效");
        }
        runCleanupCommand(List.of(dockerBinary, "rm", "--force", runtimeInstanceId));
    }

    @Override
    public void pause(String runtimeInstanceId) {
        runContainerControl("pause", runtimeInstanceId);
    }

    @Override
    public void resume(String runtimeInstanceId) {
        runContainerControl("unpause", runtimeInstanceId);
    }

    private void runContainerControl(String operation, String runtimeInstanceId) {
        List<String> command = containerControlCommand(dockerBinary, operation, runtimeInstanceId);
        try {
            ProcessResult result = runCommand(command, CONTAINER_CONTROL_TIMEOUT);
            if (result.exitCode() == 0 && !result.outputTruncated()) return;
            if ("pause".equals(operation) && !result.outputTruncated()
                    && result.output().toLowerCase(java.util.Locale.ROOT).contains("already paused")) return;
            throw new IllegalStateException("Docker 工作区进程隔离操作失败");
        } catch (IOException exception) {
            throw new IllegalStateException("Docker 工作区进程隔离命令无法启动", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Docker 工作区进程隔离操作被中断", exception);
        }
    }

    static List<String> containerControlCommand(String dockerBinary, String operation, String runtimeInstanceId) {
        if (!"pause".equals(operation) && !"unpause".equals(operation)) {
            throw new IllegalArgumentException("Docker 工作区控制操作无效");
        }
        if (runtimeInstanceId == null || !runtimeInstanceId.matches("[0-9a-f]{12,64}")) {
            throw new IllegalArgumentException("Docker 工作区实例 ID 格式无效");
        }
        return List.of(dockerBinary, operation, runtimeInstanceId);
    }

    private void removeByName(String containerName) {
        runCleanupCommand(List.of(dockerBinary, "rm", "--force", containerName));
    }

    private void cleanupAfterFailure(String containerName, Throwable originalFailure) {
        try {
            removeByName(containerName);
        } catch (RuntimeException cleanupFailure) {
            originalFailure.addSuppressed(cleanupFailure);
            log.error("Could not clean workspace container '{}' after startup failure", containerName, cleanupFailure);
        }
    }

    private void runCleanupCommand(List<String> command) {
        try {
            ProcessResult result = runCommand(command, Duration.ofMillis(Math.min(startTimeout.toMillis(), 5000)));
            if (result.exitCode() != 0) {
                if (result.outputTruncated()
                        || !result.output().toLowerCase(java.util.Locale.ROOT).contains("no such container")) {
                    throw new IllegalStateException("Docker 工作区清理失败");
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Docker 工作区清理命令无法启动", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Docker 工作区清理被中断", exception);
        } catch (RuntimeException exception) {
            throw exception;
        }
    }

    private ProcessResult runCommand(List<String> command, Duration timeout) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        OutputReader outputReader = new OutputReader(process.getInputStream(), MAX_CLI_OUTPUT_BYTES);
        Thread readerThread = new Thread(outputReader, "bm-hs-docker-cli-output");
        readerThread.setDaemon(true);
        readerThread.start();

        boolean completed;
        try {
            completed = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                process.destroyForcibly();
                process.waitFor(PROCESS_KILL_GRACE.toMillis(), TimeUnit.MILLISECONDS);
            }
            readerThread.join(PROCESS_KILL_GRACE.toMillis() + 1000);
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            closeQuietly(process.getInputStream());
            readerThread.interrupt();
            throw exception;
        }

        if (readerThread.isAlive()) {
            closeQuietly(process.getInputStream());
            readerThread.interrupt();
            readerThread.join(100);
        }
        if (outputReader.failure() != null || outputReader.result() == null) {
            throw new IllegalStateException("Docker 命令输出读取失败");
        }
        if (!completed) throw new IllegalStateException("Docker 命令执行超时");
        return new ProcessResult(process.exitValue(), outputReader.result().text(), outputReader.result().truncated());
    }

    static CapturedOutput captureOutput(InputStream input, int maxBytes) throws IOException {
        if (maxBytes <= 0) throw new IllegalArgumentException("Docker 命令输出上限必须大于 0");
        ByteArrayOutputStream captured = new ByteArrayOutputStream(Math.min(maxBytes, CLI_OUTPUT_READ_BUFFER_BYTES));
        byte[] buffer = new byte[CLI_OUTPUT_READ_BUFFER_BYTES];
        boolean truncated = false;
        int count;
        while ((count = input.read(buffer)) != -1) {
            int remaining = maxBytes - captured.size();
            int toStore = Math.min(remaining, count);
            if (toStore > 0) captured.write(buffer, 0, toStore);
            if (toStore < count) truncated = true;
        }
        return new CapturedOutput(new String(captured.toByteArray(), StandardCharsets.UTF_8), truncated);
    }

    static String parseContainerId(String output) {
        if (output == null) throw new IllegalStateException("Docker 未返回工作区容器标识");
        String[] lines = output.strip().split("\\R");
        String candidate = lines.length == 0 ? "" : lines[lines.length - 1].trim();
        if (!candidate.matches("[0-9a-f]{12,64}")) {
            throw new IllegalStateException("Docker 未返回有效的工作区容器标识");
        }
        return candidate;
    }

    private void closeQuietly(InputStream input) {
        try {
            input.close();
        } catch (IOException ignored) {
            // Closing the pipe is best-effort after the Docker CLI has been stopped.
        }
    }

    private String containerUser(WorkspaceModels.WorkspaceRecord workspace) throws IOException {
        Path storagePath = Path.of(workspace.storagePath());
        long uid = ((Number) Files.getAttribute(storagePath, "unix:uid")).longValue();
        long gid = ((Number) Files.getAttribute(storagePath, "unix:gid")).longValue();
        if (uid <= 0 || gid < 0) {
            throw new IOException("工作区目录属主无效");
        }
        return uid + ":" + gid;
    }

    private record ProcessResult(int exitCode, String output, boolean outputTruncated) {}

    record CapturedOutput(String text, boolean truncated) {}

    private static final class OutputReader implements Runnable {
        private final InputStream input;
        private final int maxBytes;
        private final AtomicReference<CapturedOutput> result = new AtomicReference<>();
        private final AtomicReference<IOException> failure = new AtomicReference<>();

        private OutputReader(InputStream input, int maxBytes) {
            this.input = input;
            this.maxBytes = maxBytes;
        }

        @Override
        public void run() {
            try {
                result.set(captureOutput(input, maxBytes));
            } catch (IOException exception) {
                failure.set(exception);
            }
        }

        private CapturedOutput result() { return result.get(); }
        private IOException failure() { return failure.get(); }
    }
}
