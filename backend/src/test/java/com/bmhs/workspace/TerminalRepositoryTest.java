package com.bmhs.workspace;

import com.bmhs.experimentcreation.ApiException;
import com.bmhs.workspace.TerminalModels.CommandRecord;
import com.bmhs.workspace.TerminalModels.TerminalSessionView;
import org.springframework.dao.DuplicateKeyException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TerminalRepositoryTest {
    private static final String CLIENT_COMMAND_ID = "77777777-7777-4777-8777-777777777777";
    private static final String CLIENT_SESSION_ID = "88888888-8888-4888-8888-888888888888";

    @Test
    void repeatedSessionRequestReturnsTheExistingOpenSessionWithoutInsertingAnother() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        stubRunningWorkspace(jdbc);
        TerminalSessionView existing = new TerminalSessionView(81L, 9L, "open", Instant.now());
        when(jdbc.query(contains("client_session_id"), ArgumentMatchers.<RowMapper<TerminalSessionView>>any(),
                eq(9L), eq(CLIENT_SESSION_ID))).thenReturn(List.of(existing));

        TerminalSessionView result = new TerminalRepository(jdbc).createSession(9L, 7L, CLIENT_SESSION_ID);

        assertEquals(existing, result);
        verify(jdbc, org.mockito.Mockito.never()).update(ArgumentMatchers.any(PreparedStatementCreator.class),
                ArgumentMatchers.any(KeyHolder.class));
    }

    @Test
    void concurrentSessionRequestRecoversTheUniqueClaimAfterDuplicateInsert() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        TerminalSessionView claimed = new TerminalSessionView(82L, 9L, "open", Instant.now());
        when(jdbc.query(contains("FROM coding_workspaces"), ArgumentMatchers.<RowMapper<String>>any(),
                eq(9L), eq(7L))).thenReturn(List.of("running"));
        when(jdbc.query(contains("client_session_id"), ArgumentMatchers.<RowMapper<TerminalSessionView>>any(),
                eq(9L), eq(CLIENT_SESSION_ID))).thenReturn(List.of(), List.of(claimed));
        when(jdbc.update(ArgumentMatchers.any(PreparedStatementCreator.class), ArgumentMatchers.any(KeyHolder.class)))
                .thenThrow(new DuplicateKeyException("same client request was already committed"));

        TerminalSessionView result = new TerminalRepository(jdbc).createSession(9L, 7L, CLIENT_SESSION_ID);

        assertEquals(claimed, result);
    }

    @Test
    void repeatedRequestCannotReuseAClosedSession() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        stubRunningWorkspace(jdbc);
        when(jdbc.query(contains("client_session_id"), ArgumentMatchers.<RowMapper<TerminalSessionView>>any(),
                eq(9L), eq(CLIENT_SESSION_ID))).thenReturn(List.of(
                new TerminalSessionView(83L, 9L, "closed", Instant.now())));

        ApiException exception = assertThrows(ApiException.class,
                () -> new TerminalRepository(jdbc).createSession(9L, 7L, CLIENT_SESSION_ID));

        assertEquals("TERMINAL_SESSION_CLOSED", exception.code());
        assertEquals(409, exception.status().value());
    }

    @Test
    void sessionCreationRejectsWorkspaceThatIsNotRunningBeforeCheckingIdempotency() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        stubWorkspaceStatus(jdbc, "starting");

        ApiException exception = assertThrows(ApiException.class,
                () -> new TerminalRepository(jdbc).createSession(9L, 7L, CLIENT_SESSION_ID));

        assertEquals("WORKSPACE_NOT_READY", exception.code());
        assertEquals(409, exception.status().value());
        verify(jdbc, org.mockito.Mockito.never()).query(contains("client_session_id"),
                ArgumentMatchers.<RowMapper<TerminalSessionView>>any(), eq(9L), eq(CLIENT_SESSION_ID));
    }

    @Test
    void sessionCreationHidesWorkspacesNotOwnedByTheStudent() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(contains("FROM coding_workspaces"), ArgumentMatchers.<RowMapper<String>>any(),
                eq(9L), eq(7L))).thenReturn(List.of());

        ApiException exception = assertThrows(ApiException.class,
                () -> new TerminalRepository(jdbc).createSession(9L, 7L, CLIENT_SESSION_ID));

        assertEquals("WORKSPACE_NOT_FOUND", exception.code());
        assertEquals(404, exception.status().value());
        verify(jdbc).query(contains("SELECT w.status"), ArgumentMatchers.<RowMapper<String>>any(), eq(9L), eq(7L));
        verify(jdbc, org.mockito.Mockito.never()).query(contains("client_session_id"),
                ArgumentMatchers.<RowMapper<TerminalSessionView>>any(), eq(9L), eq(CLIENT_SESSION_ID));
    }

    @Test
    void newCommandIsInsertedAndClaimIsCommittedIndependently() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(ArgumentMatchers.any(PreparedStatementCreator.class), ArgumentMatchers.any(KeyHolder.class)))
                .thenReturn(1);
        when(jdbc.query(contains("WHERE c.terminal_session_id = ?"), ArgumentMatchers.<RowMapper<CommandRecord>>any(),
                eq(22L), eq(CLIENT_COMMAND_ID))).thenReturn(List.of(command("pwd", "/workspace", "queued")));
        TerminalRepository repository = new TerminalRepository(jdbc);

        TerminalRepository.CommandClaim claim = repository.claimCommand(22L, CLIENT_COMMAND_ID, "pwd", "/workspace");

        assertTrue(claim.newlyClaimed());
        verify(jdbc).update(ArgumentMatchers.any(PreparedStatementCreator.class),
                ArgumentMatchers.any(KeyHolder.class));
        Transactional transaction = TerminalRepository.class.getMethod("claimCommand", long.class, String.class,
                String.class, String.class).getAnnotation(Transactional.class);
        assertEquals(Propagation.REQUIRES_NEW, transaction.propagation());
    }

    @Test
    void duplicateIdentityReusesTheExistingQueuedOrRunningClaim() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(ArgumentMatchers.any(PreparedStatementCreator.class), ArgumentMatchers.any(KeyHolder.class)))
                .thenReturn(0);
        when(jdbc.query(contains("WHERE c.terminal_session_id = ?"), ArgumentMatchers.<RowMapper<CommandRecord>>any(),
                eq(22L), eq(CLIENT_COMMAND_ID))).thenReturn(List.of(command("pwd", "/workspace", "running")));
        TerminalRepository repository = new TerminalRepository(jdbc);

        TerminalRepository.CommandClaim claim = repository.claimCommand(22L, CLIENT_COMMAND_ID, "pwd", "/workspace");

        assertFalse(claim.newlyClaimed());
        assertEquals("running", claim.command().status());
        verify(jdbc).update(ArgumentMatchers.any(PreparedStatementCreator.class),
                ArgumentMatchers.any(KeyHolder.class));
    }

    @Test
    void aReusedIdentityWithDifferentCommandOrDirectoryIsAStableConflict() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(ArgumentMatchers.any(PreparedStatementCreator.class), ArgumentMatchers.any(KeyHolder.class)))
                .thenReturn(0);
        when(jdbc.query(contains("WHERE c.terminal_session_id = ?"), ArgumentMatchers.<RowMapper<CommandRecord>>any(),
                eq(22L), eq(CLIENT_COMMAND_ID))).thenReturn(List.of(command("ls", "/workspace", "succeeded")));

        ApiException exception = assertThrows(ApiException.class, () -> new TerminalRepository(jdbc)
                .claimCommand(22L, CLIENT_COMMAND_ID, "pwd", "/workspace"));

        assertEquals("IDEMPOTENCY_KEY_REUSED", exception.code());
        assertEquals(409, exception.status().value());
    }

    @Test
    void statusLookupFiltersByOwnerAndHidesCommandsFromAnotherStudent() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(contains("r.student_id = ?"), ArgumentMatchers.<RowMapper<CommandRecord>>any(),
                eq(22L), eq(CLIENT_COMMAND_ID), eq(8L))).thenReturn(List.of());

        ApiException exception = assertThrows(ApiException.class,
                () -> new TerminalRepository(jdbc).findCommand(22L, CLIENT_COMMAND_ID, 8L));

        assertEquals("COMMAND_NOT_FOUND", exception.code());
        verify(jdbc).query(contains("JOIN experiment_runs r ON r.id = w.run_id"),
                ArgumentMatchers.<RowMapper<CommandRecord>>any(), eq(22L), eq(CLIENT_COMMAND_ID), eq(8L));
    }

    private CommandRecord command(String text, String workingDirectory, String status) {
        return new CommandRecord(49L, 22L, text, workingDirectory, status,
                "succeeded".equals(status) ? 0 : null, null, ".", Instant.now());
    }

    private void stubRunningWorkspace(JdbcTemplate jdbc) {
        stubWorkspaceStatus(jdbc, "running");
    }

    private void stubWorkspaceStatus(JdbcTemplate jdbc, String status) {
        when(jdbc.query(contains("FROM coding_workspaces"), ArgumentMatchers.<RowMapper<String>>any(),
                eq(9L), eq(7L))).thenReturn(List.of(status));
    }
}
