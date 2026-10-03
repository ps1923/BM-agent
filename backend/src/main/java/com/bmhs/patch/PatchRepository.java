package com.bmhs.patch;

import com.bmhs.experimentcreation.ApiException;
import com.bmhs.patch.PatchModels.PatchFile;
import com.bmhs.patch.PatchModels.PatchPayload;
import com.google.gson.Gson;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import java.sql.PreparedStatement;
import java.sql.Statement;

@Repository
public class PatchRepository {
    private final JdbcTemplate jdbc;
    private final Gson gson = new Gson();

    public PatchRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public PatchModels.PatchView findForStudent(long patchId, long runId, long studentId) {
        List<PatchModels.PatchView> rows = jdbc.query("""
                SELECT id, run_id, workspace_id, description, status, patch_json, created_at, applied_at, applied_snapshot_id
                FROM code_patches
                WHERE id = ? AND run_id = ? AND student_id = ?
                """, (rs, rowNum) -> toView(rs.getLong("id"), rs.getLong("run_id"), rs.getLong("workspace_id"),
                rs.getString("description"), rs.getString("status"), rs.getString("patch_json"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("applied_at") == null ? null : rs.getTimestamp("applied_at").toInstant(),
                rs.getObject("applied_snapshot_id", Long.class)),
                patchId, runId, studentId);
        if (rows.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "PATCH_NOT_FOUND", "补丁不存在或无权访问");
        return rows.get(0);
    }

    public long create(long experimentId, long runId, long workspaceId, long studentId,
                       String description, PatchPayload payload) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO code_patches
                      (experiment_id, run_id, workspace_id, student_id, description, patch_json, status)
                    VALUES (?, ?, ?, ?, ?, ?, 'pending')
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, experimentId);
            statement.setLong(2, runId);
            statement.setLong(3, workspaceId);
            statement.setLong(4, studentId);
            statement.setString(5, description);
            statement.setString(6, gson.toJson(payload));
            return statement;
        }, keys);
        Number key = keys.getKey();
        if (key == null) throw new IllegalStateException("数据库未返回补丁主键");
        return key.longValue();
    }

    public void markApplied(long patchId, long runId, long studentId, long snapshotId) {
        int changed = jdbc.update("""
                UPDATE code_patches SET status = 'applied', applied_at = CURRENT_TIMESTAMP(6), applied_snapshot_id = ?
                WHERE id = ? AND run_id = ? AND student_id = ? AND status = 'pending'
                """, snapshotId, patchId, runId, studentId);
        if (changed == 0) {
            PatchModels.PatchView patch = findForStudent(patchId, runId, studentId);
            if (!"applied".equals(patch.status())) {
                throw new ApiException(HttpStatus.CONFLICT, "PATCH_STATE_CONFLICT", "补丁当前状态不允许应用");
            }
        }
    }

    private PatchModels.PatchView toView(long id, long runId, long workspaceId, String description,
                                         String status, String json, java.time.Instant createdAt,
                                         java.time.Instant appliedAt, Long snapshotId) {
        PatchPayload payload;
        try {
            payload = gson.fromJson(json, PatchPayload.class);
        } catch (RuntimeException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "PATCH_INVALID", "补丁内容无效");
        }
        if (payload == null || payload.files() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "PATCH_INVALID", "补丁内容无效");
        }
        return new PatchModels.PatchView(id, runId, workspaceId, description, status,
                List.copyOf(payload.files()), createdAt, appliedAt, snapshotId);
    }
}
