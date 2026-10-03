package com.bmhs.workspace;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.experimentcreation.ApiException;
import com.bmhs.workspace.TerminalModels.CommandRequest;
import com.bmhs.workspace.TerminalModels.CommandView;
import com.bmhs.workspace.WorkspaceModels.WorkspaceRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TerminalServiceTest {
    private final AuthenticatedUser student = new AuthenticatedUser(7L, "student@example.com", "学生", "student");

    @Test
    void rejectsUnsafeConfiguredOutputLimit() {
        assertThrows(IllegalArgumentException.class, () -> new TerminalService(
                mock(TerminalRepository.class), mock(WorkspaceRepository.class), mock(TerminalRuntime.class),
                Duration.ofSeconds(2), 1_048_577));
    }

    @Test
    void boundsOutputByUtf8BytesWithoutSplittingCharacters() {
        TerminalService service = new TerminalService(mock(TerminalRepository.class),
                mock(WorkspaceRepository.class), mock(TerminalRuntime.class), Duration.ofSeconds(2), 4);

        assertEquals("汉\n[输出已截断]", service.boundedOutput("汉字x"));
        assertEquals("abcd\n[输出已截断]", service.boundedOutput("abcde"));
        TerminalService exactLimitService = new TerminalService(mock(TerminalRepository.class),
                mock(WorkspaceRepository.class), mock(TerminalRuntime.class), Duration.ofSeconds(2), 6);
        assertEquals("汉字", exactLimitService.boundedOutput("汉字"));
    }

    @Test
    void boundsOutputAndPersistsACompletedCommand() throws Exception {
        Path root = Files.createTempDirectory("bm-hs-terminal-output-test");
        TerminalRepository repository = mock(TerminalRepository.class);
        WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);
        TerminalRuntime runtime = mock(TerminalRuntime.class);
        WorkspaceRecord workspace = new WorkspaceRecord(9L, 3L, 2L, 7L, root.toString(),
                "test-image", "container", "running", BigDecimal.ONE, 512);
        when(repository.findSession(22L, 7L)).thenReturn(new TerminalRepository.SessionRecord(
                22L, 9L, 7L, "open", Instant.now()));
        when(workspaceRepository.findWorkspace(9L, 7L)).thenReturn(workspace);
        when(repository.createCommand(22L, "python3 -m pytest", "/workspace")).thenReturn(44L);
        when(runtime.execute(any(), any(), any(), any(Long.class), any(Integer.class)))
                .thenReturn(new TerminalRuntime.TerminalResult("succeeded", 0, "a".repeat(200)));
        when(repository.command(anyLong(), anyLong(), any())).thenReturn(
                new CommandView(44L, 22L, "python3 -m pytest", "succeeded", 0, "bounded", Instant.now()));

        TerminalService service = new TerminalService(repository, workspaceRepository, runtime,
                Duration.ofSeconds(2), 32);
        CommandView view = service.execute(student, 22L, new CommandRequest("python3 -m pytest", "."));

        assertEquals("succeeded", view.status());
        verify(repository).finishCommand(44L, "succeeded", 0, ".terminal/command-44.log");
        assertEquals(32 + "\n[输出已截断]".length(),
                Files.readString(root.resolve(".terminal/command-44.log")).length());
    }

    @Test
    void convertsUnknownRuntimeStatusToFailed() throws Exception {
        Path root = Files.createTempDirectory("bm-hs-terminal-status-test");
        TerminalRepository repository = mock(TerminalRepository.class);
        WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);
        TerminalRuntime runtime = mock(TerminalRuntime.class);
        WorkspaceRecord workspace = new WorkspaceRecord(9L, 3L, 2L, 7L, root.toString(),
                "test-image", "container", "running", BigDecimal.ONE, 512);
        when(repository.findSession(22L, 7L)).thenReturn(new TerminalRepository.SessionRecord(
                22L, 9L, 7L, "open", Instant.now()));
        when(workspaceRepository.findWorkspace(9L, 7L)).thenReturn(workspace);
        when(repository.createCommand(22L, "pwd", "/workspace")).thenReturn(45L);
        when(runtime.execute(any(), any(), any(), any(Long.class), any(Integer.class)))
                .thenReturn(new TerminalRuntime.TerminalResult("running", null, "bad status"));
        when(repository.command(anyLong(), anyLong(), any())).thenReturn(
                new CommandView(45L, 22L, "pwd", "failed", null, "bad status", Instant.now()));

        new TerminalService(repository, workspaceRepository, runtime, Duration.ofSeconds(2), 32)
                .execute(student, 22L, new CommandRequest("pwd", "."));

        verify(repository).finishCommand(45L, "failed", null, ".terminal/command-45.log");
    }

    @Test
    void closesCommandRecordWhenOutputCannotBeStored() throws Exception {
        Path file = Files.createTempFile("bm-hs-terminal-not-directory", ".tmp");
        TerminalRepository repository = mock(TerminalRepository.class);
        WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);
        TerminalRuntime runtime = mock(TerminalRuntime.class);
        WorkspaceRecord workspace = new WorkspaceRecord(9L, 3L, 2L, 7L, file.toString(),
                "test-image", "container", "running", BigDecimal.ONE, 512);
        when(repository.findSession(22L, 7L)).thenReturn(new TerminalRepository.SessionRecord(
                22L, 9L, 7L, "open", Instant.now()));
        when(workspaceRepository.findWorkspace(9L, 7L)).thenReturn(workspace);
        when(repository.createCommand(22L, "pwd", "/workspace")).thenReturn(46L);
        when(runtime.execute(any(), any(), any(), any(Long.class), any(Integer.class)))
                .thenReturn(new TerminalRuntime.TerminalResult("succeeded", 0, "output"));

        TerminalService service = new TerminalService(repository, workspaceRepository, runtime,
                Duration.ofSeconds(2), 32);
        assertThrows(ApiException.class,
                () -> service.execute(student, 22L, new CommandRequest("pwd", ".")));

        verify(repository).finishCommand(46L, "failed", null, null);
    }
}
