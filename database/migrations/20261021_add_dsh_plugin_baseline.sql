-- BM-HS DSH plugin baseline. Additive only; existing workspaces and evidence are retained.
USE `BM-sql`;

SET @run_execution_mode_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'experiment_runs' AND COLUMN_NAME = 'execution_mode'
);
SET @run_execution_mode_sql = IF(@run_execution_mode_exists = 0,
  'ALTER TABLE `experiment_runs` ADD COLUMN `execution_mode` VARCHAR(20) NOT NULL DEFAULT ''legacy'' AFTER `student_id`',
  'SELECT 1');
PREPARE run_execution_mode_stmt FROM @run_execution_mode_sql;
EXECUTE run_execution_mode_stmt;
DEALLOCATE PREPARE run_execution_mode_stmt;

SET @old_run_unique_exists = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'experiment_runs' AND INDEX_NAME = 'uq_runs_experiment_student'
);
SET @drop_old_run_unique_sql = IF(@old_run_unique_exists > 0,
  'ALTER TABLE `experiment_runs` DROP INDEX `uq_runs_experiment_student`', 'SELECT 1');
PREPARE drop_old_run_unique_stmt FROM @drop_old_run_unique_sql;
EXECUTE drop_old_run_unique_stmt;
DEALLOCATE PREPARE drop_old_run_unique_stmt;

SET @run_mode_unique_exists = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'experiment_runs' AND INDEX_NAME = 'uq_runs_experiment_student_mode'
);
SET @add_run_mode_unique_sql = IF(@run_mode_unique_exists = 0,
  'ALTER TABLE `experiment_runs` ADD UNIQUE KEY `uq_runs_experiment_student_mode` (`experiment_id`, `student_id`, `execution_mode`)',
  'SELECT 1');
PREPARE add_run_mode_unique_stmt FROM @add_run_mode_unique_sql;
EXECUTE add_run_mode_unique_stmt;
DEALLOCATE PREPARE add_run_mode_unique_stmt;

SET @teacher_review_course_nullable = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teacher_reviews'
    AND COLUMN_NAME = 'course_id' AND IS_NULLABLE = 'NO'
);
SET @teacher_review_course_nullable_sql = IF(@teacher_review_course_nullable > 0,
  'ALTER TABLE `teacher_reviews` MODIFY COLUMN `course_id` BIGINT UNSIGNED NULL', 'SELECT 1');
PREPARE teacher_review_course_nullable_stmt FROM @teacher_review_course_nullable_sql;
EXECUTE teacher_review_course_nullable_stmt;
DEALLOCATE PREPARE teacher_review_course_nullable_stmt;

CREATE TABLE IF NOT EXISTS `experiment_reviewers` (
  `experiment_id` BIGINT UNSIGNED NOT NULL,
  `teacher_id` BIGINT UNSIGNED NOT NULL,
  `assigned_by_admin_id` BIGINT UNSIGNED NOT NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`experiment_id`),
  KEY `idx_experiment_reviewers_teacher` (`teacher_id`, `experiment_id`),
  CONSTRAINT `fk_experiment_reviewers_experiment` FOREIGN KEY (`experiment_id`) REFERENCES `experiments` (`id`) ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_experiment_reviewers_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_experiment_reviewers_admin` FOREIGN KEY (`assigned_by_admin_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `dsh_instances` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT UNSIGNED NOT NULL,
  `route_key` CHAR(36) NOT NULL,
  `container_id` VARCHAR(128) NULL,
  `proxy_port` SMALLINT UNSIGNED NULL,
  `storage_volume` VARCHAR(128) NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'provisioning',
  `cpu_limit` DECIMAL(6,3) NOT NULL DEFAULT 2.000,
  `memory_limit_mb` INT UNSIGNED NOT NULL DEFAULT 4096,
  `pids_limit` INT UNSIGNED NOT NULL DEFAULT 256,
  `last_active_at` DATETIME(6) NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`), UNIQUE KEY `uq_dsh_instances_student` (`student_id`), UNIQUE KEY `uq_dsh_instances_route` (`route_key`),
  UNIQUE KEY `uq_dsh_instances_container` (`container_id`), UNIQUE KEY `uq_dsh_instances_volume` (`storage_volume`),
  KEY `idx_dsh_instances_status_active` (`status`, `last_active_at`),
  CONSTRAINT `fk_dsh_instances_student` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_dsh_instances_status` CHECK (`status` IN ('provisioning', 'running', 'stopping', 'stopped', 'error')),
  CONSTRAINT `ck_dsh_instances_cpu` CHECK (`cpu_limit` > 0),
  CONSTRAINT `ck_dsh_instances_memory` CHECK (`memory_limit_mb` > 0),
  CONSTRAINT `ck_dsh_instances_pids` CHECK (`pids_limit` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @dsh_route_key_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dsh_instances' AND COLUMN_NAME = 'route_key'
);
SET @dsh_route_key_sql = IF(@dsh_route_key_exists = 0,
  'ALTER TABLE `dsh_instances` ADD COLUMN `route_key` CHAR(36) NULL AFTER `student_id`', 'SELECT 1');
PREPARE dsh_route_key_stmt FROM @dsh_route_key_sql;
EXECUTE dsh_route_key_stmt;
DEALLOCATE PREPARE dsh_route_key_stmt;
UPDATE `dsh_instances` SET `route_key` = UUID() WHERE `route_key` IS NULL;
ALTER TABLE `dsh_instances` MODIFY COLUMN `route_key` CHAR(36) NOT NULL AFTER `student_id`;

SET @dsh_proxy_port_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dsh_instances' AND COLUMN_NAME = 'proxy_port'
);
SET @dsh_proxy_port_sql = IF(@dsh_proxy_port_exists = 0,
  'ALTER TABLE `dsh_instances` ADD COLUMN `proxy_port` SMALLINT UNSIGNED NULL AFTER `container_id`', 'SELECT 1');
PREPARE dsh_proxy_port_stmt FROM @dsh_proxy_port_sql;
EXECUTE dsh_proxy_port_stmt;
DEALLOCATE PREPARE dsh_proxy_port_stmt;

SET @dsh_route_key_index_exists = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dsh_instances' AND INDEX_NAME = 'uq_dsh_instances_route'
);
SET @dsh_route_key_index_sql = IF(@dsh_route_key_index_exists = 0,
  'ALTER TABLE `dsh_instances` ADD UNIQUE KEY `uq_dsh_instances_route` (`route_key`)', 'SELECT 1');
PREPARE dsh_route_key_index_stmt FROM @dsh_route_key_index_sql;
EXECUTE dsh_route_key_index_stmt;
DEALLOCATE PREPARE dsh_route_key_index_stmt;

SET @dsh_status_check_exists = (
  SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'dsh_instances'
    AND CONSTRAINT_NAME = 'ck_dsh_instances_status' AND CONSTRAINT_TYPE = 'CHECK'
);
SET @drop_dsh_status_check_sql = IF(@dsh_status_check_exists > 0,
  'ALTER TABLE `dsh_instances` DROP CHECK `ck_dsh_instances_status`', 'SELECT 1');
PREPARE drop_dsh_status_check_stmt FROM @drop_dsh_status_check_sql;
EXECUTE drop_dsh_status_check_stmt;
DEALLOCATE PREPARE drop_dsh_status_check_stmt;
ALTER TABLE `dsh_instances` ADD CONSTRAINT `ck_dsh_instances_status`
  CHECK (`status` IN ('provisioning', 'running', 'stopping', 'stopped', 'error'));

CREATE TABLE IF NOT EXISTS `dsh_access_tokens` (
  `token_hash` CHAR(64) NOT NULL,
  `student_id` BIGINT UNSIGNED NOT NULL,
  `instance_id` BIGINT UNSIGNED NOT NULL,
  `expires_at` DATETIME(6) NOT NULL,
  `revoked_at` DATETIME(6) NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`token_hash`),
  KEY `idx_dsh_access_tokens_student_expiry` (`student_id`, `expires_at`),
  CONSTRAINT `fk_dsh_access_tokens_student` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_dsh_access_tokens_instance` FOREIGN KEY (`instance_id`) REFERENCES `dsh_instances` (`id`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `dsh_refresh_tokens` (
  `token_hash` CHAR(64) NOT NULL,
  `student_id` BIGINT UNSIGNED NOT NULL,
  `instance_id` BIGINT UNSIGNED NOT NULL,
  `expires_at` DATETIME(6) NOT NULL,
  `revoked_at` DATETIME(6) NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`token_hash`),
  KEY `idx_dsh_refresh_tokens_instance_expiry` (`instance_id`, `expires_at`),
  CONSTRAINT `fk_dsh_refresh_tokens_student` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_dsh_refresh_tokens_instance` FOREIGN KEY (`instance_id`) REFERENCES `dsh_instances` (`id`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `dsh_run_bindings` (
  `run_id` BIGINT UNSIGNED NOT NULL,
  `experiment_id` BIGINT UNSIGNED NOT NULL,
  `student_id` BIGINT UNSIGNED NOT NULL,
  `instance_id` BIGINT UNSIGNED NOT NULL,
  `workspace_ref` VARCHAR(200) NOT NULL,
  `session_ref` VARCHAR(200) NOT NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `last_seen_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`run_id`),
  KEY `idx_dsh_run_binding_refs` (`instance_id`, `workspace_ref`, `session_ref`),
  KEY `idx_dsh_run_bindings_student_experiment` (`student_id`, `experiment_id`),
  CONSTRAINT `fk_dsh_run_bindings_run` FOREIGN KEY (`run_id`, `experiment_id`) REFERENCES `experiment_runs` (`id`, `experiment_id`) ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_dsh_run_bindings_student` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_dsh_run_bindings_instance` FOREIGN KEY (`instance_id`) REFERENCES `dsh_instances` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `code_submissions` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `run_id` BIGINT UNSIGNED NOT NULL,
  `experiment_id` BIGINT UNSIGNED NOT NULL,
  `student_id` BIGINT UNSIGNED NOT NULL,
  `node_id` BIGINT UNSIGNED NULL,
  `description` VARCHAR(500) NULL,
  `file_count` SMALLINT UNSIGNED NOT NULL,
  `total_bytes` BIGINT UNSIGNED NOT NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`), KEY `idx_code_submissions_run_created` (`run_id`, `created_at`),
  KEY `idx_code_submissions_experiment_created` (`experiment_id`, `created_at`),
  CONSTRAINT `fk_code_submissions_run` FOREIGN KEY (`run_id`, `experiment_id`) REFERENCES `experiment_runs` (`id`, `experiment_id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_code_submissions_student` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_code_submissions_node` FOREIGN KEY (`node_id`, `experiment_id`) REFERENCES `experiment_nodes` (`id`, `experiment_id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_code_submissions_nonempty` CHECK (`file_count` > 0 AND `total_bytes` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `code_submission_files` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `submission_id` BIGINT UNSIGNED NOT NULL,
  `relative_path` VARCHAR(700) NOT NULL,
  `content_text` LONGTEXT NOT NULL,
  `content_sha256` CHAR(64) NOT NULL,
  `size_bytes` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`id`), UNIQUE KEY `uq_code_submission_file_path` (`submission_id`, `relative_path`),
  CONSTRAINT `fk_code_submission_files_submission` FOREIGN KEY (`submission_id`) REFERENCES `code_submissions` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
