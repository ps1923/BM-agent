package com.bmhs.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.bmhs.auth.AuthModels.AuthenticatedUser;

@Repository
public class AuthRepository {
    public record UserRecord(long id, String email, String displayName, String role, String status,
                             String passwordHash) {}

    private final JdbcTemplate jdbc;

    public AuthRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UserRecord findUserByEmail(String email) {
        List<UserRecord> users = jdbc.query("""
                SELECT id, email, display_name, role, status, password_hash
                FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1
                """, (rs, rowNum) -> new UserRecord(rs.getLong("id"), rs.getString("email"),
                rs.getString("display_name"), rs.getString("role"), rs.getString("status"),
                rs.getString("password_hash")), email.trim());
        return users.isEmpty() ? null : users.get(0);
    }

    public AuthenticatedUser findActiveUserByTokenHash(String tokenHash) {
        List<AuthenticatedUser> users = jdbc.query("""
                SELECT u.id, u.email, u.display_name, u.role
                FROM user_sessions s
                JOIN users u ON u.id = s.user_id
                WHERE s.token_hash = ? AND s.revoked_at IS NULL
                  AND s.expires_at > CURRENT_TIMESTAMP(6) AND u.status = 'active'
                LIMIT 1
                """, (rs, rowNum) -> new AuthenticatedUser(rs.getLong("id"), rs.getString("email"),
                rs.getString("display_name"), rs.getString("role")), tokenHash);
        if (users.isEmpty()) return null;
        jdbc.update("UPDATE user_sessions SET last_seen_at = CURRENT_TIMESTAMP(6) WHERE token_hash = ?",
                tokenHash);
        return users.get(0);
    }

    public void insertSession(long userId, String tokenHash, Instant expiresAt) {
        jdbc.update("""
                INSERT INTO user_sessions (id, user_id, token_hash, expires_at)
                VALUES (?, ?, ?, ?)
                """, UUID.randomUUID().toString(), userId, tokenHash, Timestamp.from(expiresAt));
    }

    public void revokeByTokenHash(String tokenHash) {
        jdbc.update("UPDATE user_sessions SET revoked_at = CURRENT_TIMESTAMP(6) "
                + "WHERE token_hash = ? AND revoked_at IS NULL", tokenHash);
    }
}
