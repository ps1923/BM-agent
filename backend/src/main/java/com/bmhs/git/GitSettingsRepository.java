package com.bmhs.git;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class GitSettingsRepository {
    private final JdbcTemplate jdbc;

    public GitSettingsRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Optional<StoredSettings> find() {
        List<StoredSettings> rows = jdbc.query("""
                SELECT api_base_url, public_base_url, admin_token_ciphertext, webhook_secret_ciphertext,
                       webhook_target_url, updated_at
                FROM git_global_settings WHERE id = 1
                """, (rs, rowNum) -> new StoredSettings(rs.getString("api_base_url"),
                rs.getString("public_base_url"), rs.getString("admin_token_ciphertext"),
                rs.getString("webhook_secret_ciphertext"), rs.getString("webhook_target_url"),
                rs.getTimestamp("updated_at").toInstant().toString()));
        return rows.stream().findFirst();
    }

    public void save(String apiBaseUrl, String publicBaseUrl, String adminTokenCiphertext,
                     String webhookSecretCiphertext, String webhookTargetUrl, long updatedBy) {
        jdbc.update("""
                INSERT INTO git_global_settings
                  (id, api_base_url, public_base_url, admin_token_ciphertext, webhook_secret_ciphertext,
                   webhook_target_url, updated_by)
                VALUES (1, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE api_base_url = VALUES(api_base_url),
                  public_base_url = VALUES(public_base_url),
                  admin_token_ciphertext = VALUES(admin_token_ciphertext),
                  webhook_secret_ciphertext = VALUES(webhook_secret_ciphertext),
                  webhook_target_url = VALUES(webhook_target_url), updated_by = VALUES(updated_by),
                  updated_at = CURRENT_TIMESTAMP(6)
                """, apiBaseUrl, publicBaseUrl, adminTokenCiphertext, webhookSecretCiphertext,
                webhookTargetUrl, updatedBy);
    }

    public record StoredSettings(String apiBaseUrl, String publicBaseUrl, String adminTokenCiphertext,
                                 String webhookSecretCiphertext, String webhookTargetUrl, String updatedAt) {}
}
