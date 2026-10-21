package com.bmhs.workspace;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.experimentcreation.ApiException;
import com.bmhs.workspace.TerminalModels.CommandRequest;
import com.bmhs.workspace.TerminalModels.CommandRecord;
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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TerminalServiceTest {
    private final AuthenticatedUser student = new AuthenticatedUser(7L, "student@example.com", "学生", "student");

    @Test
    void rejectsMissingOrMalformedSessionIdempotencyKeyBeforeWorkspaceAccess() {
        TerminalRepository repository = mock(TerminalRepository.class);
        WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);
        TerminalService service = new TerminalService(repository, workspaceRepository, mock(TerminalRuntime.class),
                Duration.ofSeconds(2), 32);

        ApiException missing = assertThrows(ApiException.class, () -> service.createSession(student, 9L, null));
        ApiException malformed = assertThrows(ApiException.class,
                () -> service.createSession(student, 9L, "not-a-uuid"));

        assertEquals("INVALID_IDEMPOTENCY_KEY", missing.code());
        assertEquals("INVALID_IDEMPOTENCY_KEY", malformed.code());
        verifyNoInteractions(repository, workspaceRepository);
    }

    @Test
    void createsSessionWithNormalizedClientIdempotencyKey() {
        TerminalRepository repository = mock(TerminalRepository.class);
        WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);
        TerminalModels.TerminalSessionView expected = new TerminalModels.TerminalSessionView(
                15L, 9L, "open", Instant.now());
        when(workspaceRepository.findWorkspace(9L, 7L)).thenReturn(new WorkspaceRecord(9L, 3L, 2L, 7L,
                "unused", "test-image", "container", "running", BigDecimal.ONE, 512));
        when(repository.createSession(9L, 7L, "12345678-1234-4234-8234-123456789abc")).thenReturn(expected);
        TerminalService service = new TerminalService(repository, workspaceRepository, mock(TerminalRuntime.class),
                Duration.ofSeconds(2), 32);

        TerminalModels.TerminalSessionView actual = service.createSession(student, 9L,
                "12345678-1234-4234-8234-123456789ABC");

        assertEquals(expected, actual);
        verify(workspaceRepository).findWorkspace(9L, 7L);
        verify(repository).createSession(9L, 7L, "12345678-1234-4234-8234-123456789abc");
    }

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
        when(repository.claimCommand(22L, "11111111-1111-4111-8111-111111111111",
                "python3 -m pytest", "/workspace")).thenReturn(new TerminalRepository.CommandClaim(
                commandRecord(44L, "python3 -m pytest", "/workspace", "queued", null, null, root), true));
        when(repository.markCommandRunning(44L)).thenReturn(1);
        when(runtime.execute(any(), any(), any(), any(Long.class), any(Integer.class)))
                .thenReturn(new TerminalRuntime.TerminalResult("succeeded", 0, "a".repeat(200)));
        when(repository.command(anyLong(), anyLong(), any())).thenReturn(
                new CommandView(44L, 22L, "python3 -m pytest", "succeeded", 0, "bounded", Instant.now()));

        TerminalService service = new TerminalService(repository, workspaceRepository, runtime,
                Duration.ofSeconds(2), 32);
        CommandView view = service.execute(student, 22L, "11111111-1111-4111-8111-111111111111",
                new CommandRequest("python3 -m pytest", "."));

        assertEquals("succeeded", view.status());
        verify(repository).finishCommand(44L, "succeeded", 0, "command-44.log");
        verify(workspaceRepository).lockWorkspaceForMutation(9L);
        var executionOrder = inOrder(repository, runtime);
        executionOrder.verify(repository).claimCommand(22L, "11111111-1111-4111-8111-111111111111",
                "python3 -m pytest", "/workspace");
        executionOrder.verify(repository).markCommandRunning(44L);
        executionOrder.verify(runtime).execute(any(), any(), any(), any(Long.class), any(Integer.class));
        assertEquals(32 + "\n[输出已截断]".length(),
                Files.readString(root.resolveSibling(root.getFileName() + ".terminal-logs")
                        .resolve("command-44.log")).length());
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
        when(repository.claimCommand(22L, "22222222-2222-4222-8222-222222222222", "pwd", "/workspace"))
                .thenReturn(new TerminalRepository.CommandClaim(
                        commandRecord(45L, "pwd", "/workspace", "queued", null, null, root), true));
        when(repository.markCommandRunning(45L)).thenReturn(1);
        when(runtime.execute(any(), any(), any(), any(Long.class), any(Integer.class)))
                .thenReturn(new TerminalRuntime.TerminalResult("running", null, "bad status"));
        when(repository.command(anyLong(), anyLong(), any())).thenReturn(
                new CommandView(45L, 22L, "pwd", "failed", null, "bad status", Instant.now()));

        new TerminalService(repository, workspaceRepository, runtime, Duration.ofSeconds(2), 32)
                .execute(student, 22L, "22222222-2222-4222-8222-222222222222",
                        new CommandRequest("pwd", "."));

        verify(repository).finishCommand(45L, "failed", null, "command-45.log");
    }

    @Test
    void closesCommandRecordWhenOutputCannotBeStored() throws Exception {
        Path file = Files.createTempFile("bm-hs-terminal-not-directory", ".tmp");
        Path outputDirectoryAsFile = file.resolveSibling(file.getFileName() + ".terminal-logs");
        Files.createFile(outputDirectoryAsFile);
        TerminalRepository repository = mock(TerminalRepository.class);
        WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);
        TerminalRuntime runtime = mock(TerminalRuntime.class);
        WorkspaceRecord workspace = new WorkspaceRecord(9L, 3L, 2L, 7L, file.toString(),
                "test-image", "container", "running", BigDecimal.ONE, 512);
        when(repository.findSession(22L, 7L)).thenReturn(new TerminalRepository.SessionRecord(
                22L, 9L, 7L, "open", Instant.now()));
        when(workspaceRepository.findWorkspace(9L, 7L)).thenReturn(workspace);
        when(repository.claimCommand(22L, "33333333-3333-4333-8333-333333333333", "pwd", "/workspace"))
                .thenReturn(new TerminalRepository.CommandClaim(
                        commandRecord(46L, "pwd", "/workspace", "queued", null, null, file.getParent()), true));
        when(repository.markCommandRunning(46L)).thenReturn(1);
        when(runtime.execute(any(), any(), any(), any(Long.class), any(Integer.class)))
                .thenReturn(new TerminalRuntime.TerminalResult("succeeded", 0, "output"));

        TerminalService service = new TerminalService(repository, workspaceRepository, runtime,
                Duration.ofSeconds(2), 32);
        assertThrows(ApiException.class,
                () -> service.execute(student, 22L, "33333333-3333-4333-8333-333333333333",
                        new CommandRequest("pwd", ".")));

        verify(repository).finishCommand(46L, "failed", null, null);
    }

    @Test
    void rejectsMissingOrMalformedKeyBeforeLookingUpWorkspaceOrRuntime() {
        TerminalRepository repository = mock(TerminalRepository.class);
        WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);
        TerminalRuntime runtime = mock(TerminalRuntime.class);
        TerminalService service = new TerminalService(repository, workspaceRepository, runtime,
                Duration.ofSeconds(2), 32);

        ApiException missing = assertThrows(ApiException.class,
                () -> service.execute(student, 22L, null, new CommandRequest("pwd", ".")));
        ApiException malformed = assertThrows(ApiException.class,
                () -> service.execute(student, 22L, "not-a-uuid", new CommandRequest("pwd", ".")));

        assertEquals("INVALID_IDEMPOTENCY_KEY", missing.code());
        assertEquals("INVALID_IDEMPOTENCY_KEY", malformed.code());
        verifyNoInteractions(repository, workspaceRepository, runtime);
    }

    @Test
    void duplicateClaimReturnsExistingStateWithoutCallingRuntime() throws Exception {
        Path root = Files.createTempDirectory("bm-hs-terminal-existing-command");
        Path output = root.resolveSibling(root.getFileName() + ".terminal-logs").resolve("command-47.log");
        Files.createDirectories(output.getParent());
        Files.writeString(output, "previous result");
        TerminalRepository repository = mock(TerminalRepository.class);
        WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);
        TerminalRuntime runtime = mock(TerminalRuntime.class);
        when(repository.findSession(22L, 7L)).thenReturn(new TerminalRepository.SessionRecord(
                22L, 9L, 7L, "open", Instant.now()));
        when(workspaceRepository.findWorkspace(9L, 7L)).thenReturn(new WorkspaceRecord(9L, 3L, 2L, 7L,
                root.toString(), "test-image", "container", "running", BigDecimal.ONE, 512));
        when(repository.findCommandForClaim(22L, "44444444-4444-4444-8444-444444444444", 7L))
                .thenReturn(commandRecord(47L, "pwd", "/workspace", "succeeded", 0,
                        "command-47.log", root));

        CommandView view = new TerminalService(repository, workspaceRepository, runtime,
                Duration.ofSeconds(2), 32).execute(student, 22L, "44444444-4444-4444-8444-444444444444",
                new CommandRequest("pwd", "."));

        assertEquals("succeeded", view.status());
        assertEquals("previous result", view.output());
        verifyNoInteractions(runtime);
        verify(repository, org.mockito.Mockito.never())
                .claimCommand(anyLong(), any(), any(), any());
    }

    @Test
    void changedPayloadUnderExistingKeyReturnsStableConflictWithoutRuntime() {
        TerminalRepository repository = mock(TerminalRepository.class);
        WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);
        TerminalRuntime runtime = mock(TerminalRuntime.class);
        when(repository.findSession(22L, 7L)).thenReturn(new TerminalRepository.SessionRecord(
                22L, 9L, 7L, "open", Instant.now()));
        when(workspaceRepository.findWorkspace(9L, 7L)).thenReturn(new WorkspaceRecord(9L, 3L, 2L, 7L,
                ".", "test-image", "container", "running", BigDecimal.ONE, 512));
        when(repository.findCommandForClaim(22L, "55555555-5555-4555-8555-555555555555", 7L))
                .thenReturn(commandRecord(49L, "ls", "/workspace", "succeeded", 0, null, Path.of(".")));

        ApiException conflict = assertThrows(ApiException.class, () -> new TerminalService(repository,
                workspaceRepository, runtime, Duration.ofSeconds(2), 32).execute(student, 22L,
                "55555555-5555-4555-8555-555555555555", new CommandRequest("pwd", ".")));

        assertEquals("IDEMPOTENCY_KEY_REUSED", conflict.code());
        verifyNoInteractions(runtime);
        verify(repository, org.mockito.Mockito.never()).claimCommand(anyLong(), any(), any(), any());
    }

    @Test
    void changedInvalidDirectoryOnAnExistingKeyStillReturnsIdempotencyConflict() {
        TerminalRepository repository = mock(TerminalRepository.class);
        WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);
        when(repository.findSession(22L, 7L)).thenReturn(new TerminalRepository.SessionRecord(
                22L, 9L, 7L, "open", Instant.now()));
        when(workspaceRepository.findWorkspace(9L, 7L)).thenReturn(new WorkspaceRecord(9L, 3L, 2L, 7L,
                ".", "test-image", "container", "running", BigDecimal.ONE, 512));
        when(repository.findCommandForClaim(22L, "99999999-9999-4999-8999-999999999999", 7L))
                .thenReturn(commandRecord(51L, "pwd", "/workspace", "queued", null, null, Path.of(".")));

        ApiException conflict = assertThrows(ApiException.class, () -> new TerminalService(repository,
                workspaceRepository, mock(TerminalRuntime.class), Duration.ofSeconds(2), 32).execute(student,
                22L, "99999999-9999-4999-8999-999999999999", new CommandRequest("pwd", "../other")));

        assertEquals("IDEMPOTENCY_KEY_REUSED", conflict.code());
    }

    @Test
    void reconciliationIsScopedToTheAuthenticatedStudentAndCanReadPendingCommands() {
        TerminalRepository repository = mock(TerminalRepository.class);
        CommandRecord pending = commandRecord(48L, "pwd", "/workspace", "queued",
                null, null, Path.of("."));
        when(repository.findCommand(22L, "66666666-6666-4666-8666-666666666666", 7L)).thenReturn(pending);

        CommandView view = new TerminalService(repository, mock(WorkspaceRepository.class),
                mock(TerminalRuntime.class), Duration.ofSeconds(2), 32)
                .reconcile(student, 22L, "66666666-6666-4666-8666-666666666666");

        assertEquals("queued", view.status());
        assertEquals("", view.output());
        verify(repository).findCommand(22L, "66666666-6666-4666-8666-666666666666", 7L);
    }

    @Test
    void runtimeInterruptionLeavesTheDurableClaimUnresolvedAndDoesNotFinishIt() {
        TerminalRepository repository = mock(TerminalRepository.class);
        WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);
        TerminalRuntime runtime = mock(TerminalRuntime.class);
        when(repository.findSession(22L, 7L)).thenReturn(new TerminalRepository.SessionRecord(
                22L, 9L, 7L, "open", Instant.now()));
        when(workspaceRepository.findWorkspace(9L, 7L)).thenReturn(new WorkspaceRecord(9L, 3L, 2L, 7L,
                ".", "test-image", "container", "running", BigDecimal.ONE, 512));
        when(repository.claimCommand(22L, "88888888-8888-4888-8888-888888888888", "pwd", "/workspace"))
                .thenReturn(new TerminalRepository.CommandClaim(commandRecord(50L, "pwd", "/workspace",
                        "queued", null, null, Path.of(".")), true));
        when(repository.markCommandRunning(50L)).thenReturn(1);
        org.mockito.Mockito.doThrow(new IllegalStateException("runtime interrupted"))
                .when(runtime).execute(any(), any(), any(), any(Long.class), any(Integer.class));

        assertThrows(IllegalStateException.class, () -> new TerminalService(repository, workspaceRepository,
                runtime, Duration.ofSeconds(2), 32).execute(student, 22L,
                "88888888-8888-4888-8888-888888888888", new CommandRequest("pwd", ".")));

        verify(repository).markCommandRunning(50L);
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never())
                .finishCommand(anyLong(), any(), any(), any());
    }

    @Test
    void refusesToExecuteWhenDurableRunningTransitionDidNotOccur() {
        TerminalRepository repository = mock(TerminalRepository.class);
        WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);
        TerminalRuntime runtime = mock(TerminalRuntime.class);
        Path root = Path.of(".").toAbsolutePath();
        when(repository.findSession(22L, 7L)).thenReturn(new TerminalRepository.SessionRecord(
                22L, 9L, 7L, "open", Instant.now()));
        when(workspaceRepository.findWorkspace(9L, 7L)).thenReturn(new WorkspaceRecord(9L, 3L, 2L, 7L,
                root.toString(), "test-image", "container", "running", BigDecimal.ONE, 512));
        when(repository.claimCommand(22L, "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa", "pwd", "/workspace"))
                .thenReturn(new TerminalRepository.CommandClaim(commandRecord(52L, "pwd", "/workspace",
                        "queued", null, null, root), true));
        when(repository.markCommandRunning(52L)).thenReturn(0);

        assertThrows(IllegalStateException.class, () -> new TerminalService(repository, workspaceRepository,
                runtime, Duration.ofSeconds(2), 32).execute(student, 22L,
                "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa", new CommandRequest("pwd", ".")));

        verifyNoInteractions(runtime);
    }

    private CommandRecord commandRecord(long id, String text, String directory, String status,
                                                            Integer exitCode, String outputKey, Path root) {
        return new CommandRecord(id, 22L, text, directory, status, exitCode, outputKey,
                root.toString(), Instant.now());
    }
}
