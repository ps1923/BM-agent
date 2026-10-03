package com.bmhs.workspace;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;

@Component
public class DockerTerminalRuntime implements TerminalRuntime {
    private static final Duration KILL_GRACE = Duration.ofSeconds(1);
    private static final int OUTPUT_READ_BUFFER_BYTES = 8192;
    private final String dockerBinary;

    public DockerTerminalRuntime(@Value("${bm-hs.workspace.docker-binary:docker}") String dockerBinary) {
        this.dockerBinary = dockerBinary;
    }

    @Override
    public TerminalResult execute(WorkspaceModels.WorkspaceRecord workspace, List<String> command,
                                  String workingDirectory, long timeoutMs, int maxOutputBytes) {
        if (workspace.runtimeInstanceId() == null || workspace.runtimeInstanceId().isBlank()) {
            return new TerminalResult("failed", null, "工作区容器标识不存在");
        }
        List<String> args = buildCommand(dockerBinary, workspace, command, workingDirectory, timeoutMs);
        Process process = null;
        Thread readerThread = null;
        try {
            process = new ProcessBuilder(args).redirectErrorStream(true).start();
            OutputReader reader = new OutputReader(process.getInputStream(), maxOutputBytes);
            readerThread = new Thread(reader, "bm-hs-terminal-output");
            readerThread.setDaemon(true);
            readerThread.start();
            boolean timedOut = !process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (timedOut) {
                process.destroyForcibly();
                process.waitFor(KILL_GRACE.toMillis(), TimeUnit.MILLISECONDS);
            }
            readerThread.join(KILL_GRACE.toMillis() + 1000);
            if (readerThread.isAlive()) {
                closeQuietly(process.getInputStream());
                readerThread.interrupt();
                readerThread.join(100);
            }
            if (reader.failure() != null || reader.result() == null) {
                return new TerminalResult("failed", null, "容器输出读取失败");
            }
            if (timedOut) return new TerminalResult("cancelled", null, "命令执行超时，已终止");
            String output = outputText(reader.result());
            int exitCode = process.exitValue();
            if (exitCode == 124 || exitCode == 137) {
                return new TerminalResult("cancelled", null, "命令执行超时，已终止");
            }
            return new TerminalResult(exitCode == 0 ? "succeeded" : "failed", exitCode, output);
        } catch (IOException exception) {
            return new TerminalResult("failed", null, "容器命令执行失败");
        } catch (InterruptedException exception) {
            if (process != null) process.destroyForcibly();
            if (readerThread != null) {
                readerThread.interrupt();
                if (process != null) closeQuietly(process.getInputStream());
            }
            Thread.currentThread().interrupt();
            return new TerminalResult("cancelled", null, "命令执行被中断");
        }
    }

    static CapturedOutput captureOutput(InputStream input, int maxBytes) throws IOException {
        if (maxBytes <= 0) throw new IllegalArgumentException("输出上限必须大于 0");
        ByteArrayOutputStream captured = new ByteArrayOutputStream(Math.min(maxBytes, OUTPUT_READ_BUFFER_BYTES));
        byte[] buffer = new byte[OUTPUT_READ_BUFFER_BYTES];
        boolean truncated = false;
        int count;
        while ((count = input.read(buffer)) != -1) {
            int remaining = maxBytes - captured.size();
            int toStore = Math.min(remaining, count);
            if (toStore > 0) captured.write(buffer, 0, toStore);
            if (toStore < count) truncated = true;
        }
        return new CapturedOutput(captured.toByteArray(), truncated);
    }

    static String outputText(CapturedOutput captured) {
        byte[] bytes = captured.bytes();
        int validLength = captured.truncated() ? completeUtf8PrefixLength(bytes) : bytes.length;
        String output = new String(bytes, 0, validLength, StandardCharsets.UTF_8);
        return captured.truncated() ? output + "\n[输出已截断]" : output;
    }

    private static int completeUtf8PrefixLength(byte[] bytes) {
        int start = bytes.length - 1;
        while (start >= 0 && (bytes[start] & 0b1100_0000) == 0b1000_0000) start--;
        if (start < 0) return bytes.length;
        int lead = bytes[start] & 0xff;
        int expectedLength = lead < 0x80 ? 1
                : (lead & 0b1110_0000) == 0b1100_0000 ? 2
                : (lead & 0b1111_0000) == 0b1110_0000 ? 3
                : (lead & 0b1111_1000) == 0b1111_0000 ? 4 : 1;
        return start + expectedLength > bytes.length ? start : bytes.length;
    }

    private void closeQuietly(InputStream input) {
        try {
            input.close();
        } catch (IOException ignored) {
            // Closing the pipe is best-effort after the docker process was forcibly stopped.
        }
    }

    static List<String> buildCommand(String dockerBinary, WorkspaceModels.WorkspaceRecord workspace,
                                     List<String> command, String workingDirectory, long timeoutMs) {
        long guardedTimeoutMs = Math.max(1000, timeoutMs - KILL_GRACE.toMillis() - 1000);
        long guardedTimeoutSeconds = Math.max(1, (guardedTimeoutMs + 999) / 1000);
        List<String> args = new java.util.ArrayList<>(List.of(dockerBinary, "exec", "--workdir",
                workingDirectory, workspace.runtimeInstanceId(), "timeout", "--signal=TERM",
                "--kill-after=1s", guardedTimeoutSeconds + "s"));
        args.addAll(command);
        return List.copyOf(args);
    }

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

    record CapturedOutput(byte[] bytes, boolean truncated) {}
}
