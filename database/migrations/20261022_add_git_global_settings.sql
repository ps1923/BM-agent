-- Add the shared administrator-managed Gitea settings row. Secret values are
-- stored only as AES-GCM ciphertext; the encryption key remains environment-only.
USE `BM-sql`;

CREATE TABLE IF NOT EXISTS `git_global_settings` (
  `id` TINYINT UNSIGNED NOT NULL DEFAULT 1,
  `api_base_url` VARCHAR(500) NOT NULL,
  `public_base_url` VARCHAR(500) NOT NULL,
  `admin_token_ciphertext` TEXT NULL,
  `webhook_secret_ciphertext` TEXT NULL,
  `webhook_target_url` VARCHAR(1000) NOT NULL,
  `updated_by` BIGINT UNSIGNED NOT NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  CONSTRAINT `ck_git_global_settings_singleton` CHECK (`id` = 1),
  CONSTRAINT `fk_git_global_settings_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `users` (`id`)
    ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
