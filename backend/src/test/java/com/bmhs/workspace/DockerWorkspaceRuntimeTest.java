package com.bmhs.workspace;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerWorkspaceRuntimeTest {
    @Test
    void drainsLargeCliOutputWhileRetainingOnlyTheConfiguredLimit() throws Exception {
        byte[] source = "x".repeat(100_000).getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream stream = new ByteArrayInputStream(source);

        DockerWorkspaceRuntime.CapturedOutput captured = DockerWorkspaceRuntime.captureOutput(stream, 64);

        assertEquals("x".repeat(64), captured.text());
        assertTrue(captured.truncated());
        assertEquals(0, stream.available());
    }

    @Test
    void acceptsOnlyDockerContainerIdsFromTheLastOutputLine() {
        String id = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

        assertEquals(id, DockerWorkspaceRuntime.parseContainerId("warning from docker\n" + id + "\n"));
        assertThrows(IllegalStateException.class,
                () -> DockerWorkspaceRuntime.parseContainerId("container started"));
        assertThrows(IllegalStateException.class,
                () -> DockerWorkspaceRuntime.parseContainerId("0123456789; rm -rf /"));
    }

    @Test
    void rejectsNonPositiveStartupTimeoutConfiguration() {
        assertThrows(IllegalArgumentException.class,
                () -> new DockerWorkspaceRuntime("docker", Duration.ZERO));
    }

    @Test
    void containerPauseAndResumeCommandsAcceptOnlyKnownOperationsAndDockerIds() {
        String id = "0123456789abcdef";

        assertEquals(java.util.List.of("docker", "pause", id),
                DockerWorkspaceRuntime.containerControlCommand("docker", "pause", id));
        assertEquals(java.util.List.of("docker", "unpause", id),
                DockerWorkspaceRuntime.containerControlCommand("docker", "unpause", id));
        assertThrows(IllegalArgumentException.class,
                () -> DockerWorkspaceRuntime.containerControlCommand("docker", "restart", id));
        assertThrows(IllegalArgumentException.class,
                () -> DockerWorkspaceRuntime.containerControlCommand("docker", "pause", "container;rm"));
    }
}
