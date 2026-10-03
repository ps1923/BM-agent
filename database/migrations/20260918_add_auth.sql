-- BE-01 authentication migration for an existing BM-sql installation.
-- Existing accounts without a password_hash remain unable to log in until provisioned.
USE `BM-sql`;

SET @has_password_hash = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'BM-sql' AND TABLE_NAME = 'users' AND COLUMN_NAME = 'password_hash'
);
SET @ddl = IF(@has_password_hash = 0,
  'ALTER TABLE `users` ADD COLUMN `password_hash` VARCHAR(255) NULL AFTER `display_name`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `user_sessions` (
  `id` CHAR(36) NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `token_hash` CHAR(64) NOT NULL,
  `expires_at` DATETIME(6) NOT NULL,
  `revoked_at` DATETIME(6) NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `last_seen_at` DATETIME(6) NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_sessions_token_hash` (`token_hash`),
  KEY `idx_user_sessions_user_active` (`user_id`, `expires_at`, `revoked_at`),
  CONSTRAINT `fk_user_sessions_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
