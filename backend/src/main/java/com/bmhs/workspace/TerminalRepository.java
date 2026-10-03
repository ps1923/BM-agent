package com.bmhs.workspace;

import com.bmhs.experimentcreation.ApiException;
import com.bmhs.workspace.TerminalModels.CommandView;
import com.bmhs.workspace.TerminalModels.TerminalSessionView;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;

@Repository
public class TerminalRepository {
    public record SessionRecord(long id, long workspaceId, long studentId, String status, Instant createdAt) {}

    private final JdbcTemplate jdbc;

    public TerminalRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public TerminalSessionView createSession(long workspaceId, long studentId) {
        List<SessionRecord> workspace = jdbc.query("""
                SELECT w.id AS workspace_id, r.student_id, w.status, w.created_at
                FROM coding_workspaces w JOIN experiment_runs r ON r.id = w.run_id
                WHERE w.id = ? AND r.student_id = ?
                """, (rs, rowNum) -> new SessionRecord(rs.getLong("workspace_id"), rs.getLong("student_id"),
                rs.getLong("student_id"), rs.getString("status"), rs.getTimestamp("created_at").toInstant()),
                workspaceId, studentId);
        if (workspace.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "WORKSPACE_NOT_FOUND", "工作区不存在");
        if (!"running".equals(workspace.get(0).status())) {
            throw new ApiException(HttpStatus.CONFLICT, "WORKSPACE_NOT_READY", "工作区尚未就绪");
        }
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO terminal_sessions (workspace_id, shell, working_directory, status) "
                            + "VALUES (?, 'restricted', '/workspace', 'open')",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, workspaceId);
            return statement;
        }, keys);
        long id = keys.getKey().longValue();
        return new TerminalSessionView(id, workspaceId, "open", Instant.now());
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
        if (!"open".equals(sessions.get(0).status())) {
            throw new ApiException(HttpStatus.CONFLICT, "TERMINAL_SESSION_CLOSED", "终端会话已关闭");
        }
        return sessions.get(0);
    }

    public long createCommand(long terminalSessionId, String command, String workingDirectory) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO command_runs
                      (terminal_session_id, command_text, working_directory, status, started_at)
                    VALUES (?, ?, ?, 'queued', CURRENT_TIMESTAMP(6))
                """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, terminalSessionId);
            statement.setString(2, command);
            statement.setString(3, workingDirectory);
            return statement;
        }, keys);
        return keys.getKey().longValue();
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
}
