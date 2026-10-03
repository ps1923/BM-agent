package com.bmhs.workspace;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerTerminalRuntimeTest {
    @Test
    void drainsUnlimitedCommandOutputButStoresOnlyTheConfiguredByteLimit() throws Exception {
        byte[] source = "x".repeat(100_000).getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream stream = new ByteArrayInputStream(source);

        DockerTerminalRuntime.CapturedOutput output = DockerTerminalRuntime.captureOutput(stream, 64);

        assertEquals(64, output.bytes().length);
        assertTrue(output.truncated());
        assertEquals(0, stream.available());
        assertEquals("x".repeat(64) + "\n[输出已截断]", DockerTerminalRuntime.outputText(output));
    }

    @Test
    void byteLimitDoesNotSplitUtf8Characters() throws Exception {
        ByteArrayInputStream stream = new ByteArrayInputStream("汉字x".getBytes(StandardCharsets.UTF_8));

        DockerTerminalRuntime.CapturedOutput output = DockerTerminalRuntime.captureOutput(stream, 4);

        assertEquals(4, output.bytes().length);
        assertTrue(output.truncated());
        assertEquals("汉\n[输出已截断]", DockerTerminalRuntime.outputText(output));
    }

    @Test
    void exactLimitIsNotReportedAsTruncated() throws Exception {
        ByteArrayInputStream stream = new ByteArrayInputStream("1234".getBytes(StandardCharsets.UTF_8));

        DockerTerminalRuntime.CapturedOutput output = DockerTerminalRuntime.captureOutput(stream, 4);

        assertFalse(output.truncated());
        assertEquals("1234", DockerTerminalRuntime.outputText(output));
    }

    @Test
    void wrapsCommandsWithAnInContainerTimeoutGuard() {
        WorkspaceModels.WorkspaceRecord workspace = new WorkspaceModels.WorkspaceRecord(
                9L, 3L, 2L, 7L, "/var/lib/bm-hs/workspaces/run-5",
                "bm-hs/workspace:latest", "container-5", "running", BigDecimal.ONE, 1024);

        assertEquals(List.of("docker", "exec", "--workdir", "/workspace", "container-5",
                        "timeout", "--signal=TERM", "--kill-after=1s", "28s",
                        "python3", "-m", "timeit", "pass"),
                DockerTerminalRuntime.buildCommand("docker", workspace,
                        List.of("python3", "-m", "timeit", "pass"), "/workspace", 30000));
    }
}
