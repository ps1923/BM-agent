package com.bmhs.dsh;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public class DshTokenRepository {
    private final JdbcTemplate jdbc;

    public DshTokenRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void save(String tokenHash, long studentId, long instanceId, Instant expiresAt) {
        jdbc.update("""
                INSERT INTO dsh_access_tokens (token_hash, student_id, instance_id, expires_at)
                VALUES (?, ?, ?, ?)
                """, tokenHash, studentId, instanceId, Timestamp.from(expiresAt));
    }

    public void saveRefresh(String tokenHash, long studentId, long instanceId, Instant expiresAt) {
        jdbc.update("""
                INSERT INTO dsh_refresh_tokens (token_hash, student_id, instance_id, expires_at)
                VALUES (?, ?, ?, ?)
                """, tokenHash, studentId, instanceId, Timestamp.from(expiresAt));
    }

    public DshPrincipal consumeRefresh(String tokenHash, Instant now) {
        List<DshPrincipal> rows = jdbc.query("""
                SELECT t.student_id, t.instance_id, t.expires_at
                FROM dsh_refresh_tokens t
                JOIN dsh_instances i ON i.id = t.instance_id AND i.student_id = t.student_id
                JOIN users u ON u.id = t.student_id AND u.status = 'active'
                WHERE t.token_hash = ? AND t.revoked_at IS NULL AND t.expires_at > ?
                  AND i.status = 'running'
                FOR UPDATE
                """, (rs, rowNum) -> new DshPrincipal(rs.getLong("student_id"), rs.getLong("instance_id"),
                rs.getTimestamp("expires_at").toInstant()), tokenHash, Timestamp.from(now));
        if (rows.isEmpty()) return null;
        int consumed = jdbc.update("""
                UPDATE dsh_refresh_tokens SET revoked_at = ?
                WHERE token_hash = ? AND revoked_at IS NULL AND expires_at > ?
                """, Timestamp.from(now), tokenHash, Timestamp.from(now));
        if (consumed != 1) return null;
        jdbc.update("UPDATE dsh_instances SET last_active_at = CURRENT_TIMESTAMP(6) WHERE id = ? AND status = 'running'",
                rows.get(0).instanceId());
        return rows.get(0);
    }

    public DshPrincipal authenticate(String tokenHash, Instant now) {
        List<DshPrincipal> rows = jdbc.query("""
                SELECT t.student_id, t.instance_id, t.expires_at
                FROM dsh_access_tokens t
                JOIN dsh_instances i ON i.id = t.instance_id AND i.student_id = t.student_id
                JOIN users u ON u.id = t.student_id AND u.status = 'active'
                WHERE t.token_hash = ? AND t.revoked_at IS NULL AND t.expires_at > ?
                  AND i.status = 'running'
                """, (rs, rowNum) -> new DshPrincipal(rs.getLong("student_id"), rs.getLong("instance_id"),
                rs.getTimestamp("expires_at").toInstant()), tokenHash, Timestamp.from(now));
        if (rows.isEmpty()) return null;
        jdbc.update("UPDATE dsh_instances SET last_active_at = CURRENT_TIMESTAMP(6) WHERE id = ? AND status = 'running'",
                rows.get(0).instanceId());
        return rows.get(0);
    }

    public void revokeStudentTokens(long studentId, long instanceId) {
        jdbc.update("UPDATE dsh_access_tokens SET revoked_at = CURRENT_TIMESTAMP(6) "
                + "WHERE student_id = ? AND instance_id = ? AND revoked_at IS NULL", studentId, instanceId);
    }

    public void revokeStudentRefreshTokens(long studentId, long instanceId) {
        jdbc.update("UPDATE dsh_refresh_tokens SET revoked_at = CURRENT_TIMESTAMP(6) "
                + "WHERE student_id = ? AND instance_id = ? AND revoked_at IS NULL", studentId, instanceId);
    }
}
