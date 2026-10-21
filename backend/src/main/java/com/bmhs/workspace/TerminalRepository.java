package com.bmhs.workspace;

import com.bmhs.experimentcreation.ApiException;
import com.bmhs.workspace.TerminalModels.CommandRecord;
import com.bmhs.workspace.TerminalModels.CommandView;
import com.bmhs.workspace.TerminalModels.TerminalSessionView;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;

@Repository
public class TerminalRepository {
    public record SessionRecord(long id, long workspaceId, long studentId, String status, Instant createdAt) {}
    public record CommandClaim(CommandRecord command, boolean newlyClaimed) {}

    private final JdbcTemplate jdbc;

    public TerminalRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public TerminalSessionView createSession(long workspaceId, long studentId, String clientSessionId) {
        List<String> workspaceStatuses = jdbc.query("""
                SELECT w.status
                FROM coding_workspaces w JOIN experiment_runs r ON r.id = w.run_id
                WHERE w.id = ? AND r.student_id = ?
                """, (rs, rowNum) -> rs.getString("status"),
                workspaceId, studentId);
        if (workspaceStatuses.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "WORKSPACE_NOT_FOUND", "工作区不存在");
        }
        if (!"running".equals(workspaceStatuses.get(0))) {
            throw new ApiException(HttpStatus.CONFLICT, "WORKSPACE_NOT_READY", "工作区尚未就绪");
        }
        TerminalSessionView existing = findSessionByClientId(workspaceId, clientSessionId);
        if (existing != null) return requireOpenSession(existing);
        try {
            KeyHolder keys = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO terminal_sessions (workspace_id, client_session_id, shell, working_directory, status) "
                                + "VALUES (?, ?, 'restricted', '/workspace', 'open')",
                        Statement.RETURN_GENERATED_KEYS);
                statement.setLong(1, workspaceId);
                statement.setString(2, clientSessionId);
                return statement;
            }, keys);
            long id = keys.getKey().longValue();
            return new TerminalSessionView(id, workspaceId, "open", Instant.now());
        } catch (DuplicateKeyException duplicate) {
            TerminalSessionView claimed = findSessionByClientId(workspaceId, clientSessionId);
            if (claimed == null) throw duplicate;
            return requireOpenSession(claimed);
        }
    }

    private TerminalSessionView findSessionByClientId(long workspaceId, String clientSessionId) {
        List<TerminalSessionView> sessions = jdbc.query("""
                SELECT id, workspace_id, status, started_at
                FROM terminal_sessions
                WHERE workspace_id = ? AND client_session_id = ?
                """, (rs, rowNum) -> new TerminalSessionView(rs.getLong("id"), rs.getLong("workspace_id"),
                rs.getString("status"), rs.getTimestamp("started_at").toInstant()), workspaceId, clientSessionId);
        return sessions.isEmpty() ? null : sessions.get(0);
    }

    private TerminalSessionView requireOpenSession(TerminalSessionView session) {
        if (!"open".equals(session.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "TERMINAL_SESSION_CLOSED", "终端会话已关闭");
        }
        return session;
    }

    public SessionRecord findSession(long sessionId, long studentId) {
        List<SessionRecord> sessions = jdbc.query("""
                SELECT t.id, t.workspace_id, r.student_id, t.status, t.started_at AS created_at
                FROM terminal_sessions t JOIN coding_workspaces w ON w.id = t.workspace_id
                JOIN experiment_runs r ON r.id = w.run_id
                WHERE t.id = ? AND r.student_id = ?
                """, (rs, rowNum) -> new SessionRecord(rs.getLong("id"), rs.getLong("workspace_id"),
                rs.getLong("student_id"), rs.getString("status"), rs.getTimestamp("created_at").toInstant()),
                sessionId, studentId);
        if (sessions.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "TERMINAL_SESSION_NOT_FOUND", "终端会话不存在");
        return sessions.get(0);
    }

    public CommandRecord findCommandForClaim(long terminalSessionId, String clientCommandId, long studentId) {
        List<CommandRecord> commands = jdbc.query("""
                SELECT c.id, c.terminal_session_id, c.command_text, c.working_directory, c.status, c.exit_code,
                       c.output_storage_key, w.storage_path, c.created_at
                FROM command_runs c JOIN terminal_sessions t ON t.id = c.terminal_session_id
                JOIN coding_workspaces w ON w.id = t.workspace_id
                JOIN experiment_runs r ON r.id = w.run_id
                WHERE c.terminal_session_id = ? AND c.client_command_id = ? AND r.student_id = ?
                FOR UPDATE
                """, this::toCommandRecord, terminalSessionId, clientCommandId, studentId);
        return commands.isEmpty() ? null : commands.get(0);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CommandClaim claimCommand(long terminalSessionId, String clientCommandId, String command,
                                     String workingDirectory) {
        int inserted = jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT IGNORE INTO command_runs
                      (terminal_session_id, client_command_id, command_text, working_directory, status, started_at)
                    VALUES (?, ?, ?, ?, 'queued', CURRENT_TIMESTAMP(6))
                """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, terminalSessionId);
            statement.setString(2, clientCommandId);
            statement.setString(3, command);
            statement.setString(4, workingDirectory);
            return statement;
        }, new GeneratedKeyHolder());
        List<CommandRecord> commands = findCommandRecords(terminalSessionId, clientCommandId);
        if (commands.isEmpty()) throw new IllegalStateException("终端命令认领未能持久化");
        CommandRecord commandRecord = commands.get(0);
        if (!commandRecord.commandText().equals(command)
                || !commandRecord.workingDirectory().equals(workingDirectory)) {
            throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED", "幂等键已用于不同的命令内容");
        }
        return new CommandClaim(commandRecord, inserted == 1);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int markCommandRunning(long commandId) {
        return jdbc.update("UPDATE command_runs SET status = 'running', started_at = CURRENT_TIMESTAMP(6) "
                + "WHERE id = ? AND status = 'queued'", commandId);
    }

    public void finishCommand(long commandId, String status, Integer exitCode, String outputStorageKey) {
        jdbc.update("UPDATE command_runs SET status = ?, exit_code = ?, output_storage_key = ?, "
                + "finished_at = CURRENT_TIMESTAMP(6) WHERE id = ?", status, exitCode, outputStorageKey, commandId);
    }

    public CommandView command(long commandId, long studentId, String output) {
        List<CommandView> commands = jdbc.query("""
                SELECT c.id, c.terminal_session_id, c.command_text, c.status, c.exit_code, c.created_at
                FROM command_runs c JOIN terminal_sessions t ON t.id = c.terminal_session_id
                JOIN coding_workspaces w ON w.id = t.workspace_id JOIN experiment_runs r ON r.id = w.run_id
                WHERE c.id = ? AND r.student_id = ?
                """, (rs, rowNum) -> new CommandView(rs.getLong("id"), rs.getLong("terminal_session_id"),
                rs.getString("command_text"), rs.getString("status"), rs.getObject("exit_code", Integer.class),
                output, rs.getTimestamp("created_at").toInstant()), commandId, studentId);
        if (commands.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "COMMAND_NOT_FOUND", "命令记录不存在");
        return commands.get(0);
    }

    public CommandRecord findCommand(long terminalSessionId, String clientCommandId, long studentId) {
        List<CommandRecord> commands = jdbc.query("""
                SELECT c.id, c.terminal_session_id, c.command_text, c.working_directory, c.status, c.exit_code,
                       c.output_storage_key, w.storage_path, c.created_at
                FROM command_runs c JOIN terminal_sessions t ON t.id = c.terminal_session_id
                JOIN coding_workspaces w ON w.id = t.workspace_id
                JOIN experiment_runs r ON r.id = w.run_id
                WHERE c.terminal_session_id = ? AND c.client_command_id = ? AND r.student_id = ?
                """, this::toCommandRecord, terminalSessionId, clientCommandId, studentId);
        if (commands.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "COMMAND_NOT_FOUND", "命令记录不存在");
        return commands.get(0);
    }

    private List<CommandRecord> findCommandRecords(long terminalSessionId, String clientCommandId) {
        return jdbc.query("""
                SELECT c.id, c.terminal_session_id, c.command_text, c.working_directory, c.status, c.exit_code,
                       c.output_storage_key, w.storage_path, c.created_at
                FROM command_runs c JOIN terminal_sessions t ON t.id = c.terminal_session_id
                JOIN coding_workspaces w ON w.id = t.workspace_id
                WHERE c.terminal_session_id = ? AND c.client_command_id = ?
                """, this::toCommandRecord, terminalSessionId, clientCommandId);
    }

    private CommandRecord toCommandRecord(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new CommandRecord(rs.getLong("id"), rs.getLong("terminal_session_id"),
                rs.getString("command_text"), rs.getString("working_directory"), rs.getString("status"),
                rs.getObject("exit_code", Integer.class), rs.getString("output_storage_key"),
                rs.getString("storage_path"), rs.getTimestamp("created_at").toInstant());
    }
}
