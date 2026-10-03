package com.bmhs.diagnosis;

import com.bmhs.experimentcreation.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public class DiagnosisRepository {
    private final JdbcTemplate jdbc;

    public DiagnosisRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public long create(long runId, long experimentId, long workspaceId, long studentId) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO diagnosis_tasks
                      (run_id, experiment_id, workspace_id, student_id, status)
                    VALUES (?, ?, ?, ?, 'queued')
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, runId); statement.setLong(2, experimentId);
            statement.setLong(3, workspaceId); statement.setLong(4, studentId);
            return statement;
        }, keys);
        Number key = keys.getKey();
        if (key == null) throw new IllegalStateException("数据库未返回诊断任务主键");
        return key.longValue();
    }

    public Long findActiveId(long runId, long studentId) {
        List<Long> ids = jdbc.query("""
                SELECT id FROM diagnosis_tasks
                WHERE run_id = ? AND student_id = ? AND status IN ('queued', 'running')
                ORDER BY id DESC LIMIT 1
                """, (rs, rowNum) -> rs.getLong(1), runId, studentId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    public DiagnosisModels.DiagnosisView findForStudent(long id, long studentId) {
        List<DiagnosisModels.DiagnosisView> rows = jdbc.query("""
                SELECT id, run_id, status, result_json, created_at, started_at, completed_at, error_message
                FROM diagnosis_tasks WHERE id = ? AND student_id = ?
                """, (rs, rowNum) -> DiagnosisJson.toView(rs.getLong("id"), rs.getLong("run_id"),
                rs.getString("status"), rs.getString("result_json"), instant(rs.getTimestamp("created_at")),
                instant(rs.getTimestamp("started_at")), instant(rs.getTimestamp("completed_at")),
                rs.getString("error_message")), id, studentId);
        if (rows.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "DIAGNOSIS_NOT_FOUND", "诊断任务不存在或无权访问");
        return rows.get(0);
    }

    public TaskRecord findTask(long id) {
        List<TaskRecord> rows = jdbc.query("""
                SELECT id, run_id, experiment_id, workspace_id, student_id, status
                FROM diagnosis_tasks WHERE id = ?
                """, (rs, rowNum) -> new TaskRecord(rs.getLong("id"), rs.getLong("run_id"),
                rs.getLong("experiment_id"), rs.getLong("workspace_id"), rs.getLong("student_id"),
                rs.getString("status")), id);
        if (rows.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "DIAGNOSIS_NOT_FOUND", "诊断任务不存在");
        return rows.get(0);
    }

    public void markRunning(long id) { jdbc.update("UPDATE diagnosis_tasks SET status = 'running', started_at = CURRENT_TIMESTAMP(6) WHERE id = ? AND status = 'queued'", id); }
    public void complete(long id, String resultJson) { jdbc.update("UPDATE diagnosis_tasks SET status = 'completed', result_json = ?, completed_at = CURRENT_TIMESTAMP(6) WHERE id = ?", resultJson, id); }
    public void fail(long id, String safeError) { jdbc.update("UPDATE diagnosis_tasks SET status = 'failed', error_message = ?, completed_at = CURRENT_TIMESTAMP(6) WHERE id = ?", safeError, id); }

    private static Instant instant(Timestamp timestamp) { return timestamp == null ? null : timestamp.toInstant(); }

    public record TaskRecord(long id, long runId, long experimentId, long workspaceId, long studentId, String status) {}
}
