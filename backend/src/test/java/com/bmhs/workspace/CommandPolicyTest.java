package com.bmhs.workspace;

import com.bmhs.experimentcreation.ApiException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommandPolicyTest {
    @Test
    void acceptsOnlyKnownExecutableAndReturnsArgumentList() {
        assertEquals(List.of("python3", "-m", "pytest"),
                CommandPolicy.parse("python3 -m pytest"));
    }

    @Test
    void rejectsShellCompositionAndProtectedPaths() {
        ApiException shell = assertThrows(ApiException.class,
                () -> CommandPolicy.parse("mvn test && cat /etc/passwd"));
        assertEquals("COMMAND_REJECTED", shell.code());

        ApiException path = assertThrows(ApiException.class,
                () -> CommandPolicy.parse("cat ../secret.txt"));
        assertEquals("COMMAND_REJECTED", path.code());
    }

    @Test
    void rejectsAbsoluteAndTraversalWorkingDirectories() {
        assertThrows(ApiException.class, () -> CommandPolicy.workingDirectory("/tmp"));
        assertThrows(ApiException.class, () -> CommandPolicy.workingDirectory("src/../"));
        assertEquals("/workspace/src/main", CommandPolicy.workingDirectory("src/main"));
    }

    @Test
    void allowsWorkspaceFilesButRejectsImageSystemFiles() {
        assertEquals(List.of("cat", "/workspace/src/Main.java"),
                CommandPolicy.parse("cat /workspace/src/Main.java"));
        assertThrows(ApiException.class, () -> CommandPolicy.parse("cat /etc/hosts"));
    }
}
