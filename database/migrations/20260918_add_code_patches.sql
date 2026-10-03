-- BE-08 confirmed patch application migration.
-- Safe to run repeatedly against a compatible BM-sql installation.
USE `BM-sql`;

CREATE TABLE IF NOT EXISTS `code_patches` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `experiment_id` BIGINT UNSIGNED NOT NULL,
  `run_id` BIGINT UNSIGNED NOT NULL,
  `workspace_id` BIGINT UNSIGNED NOT NULL,
  `student_id` BIGINT UNSIGNED NOT NULL,
  `description` VARCHAR(500) NULL,
  `patch_json` MEDIUMTEXT NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending',
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `applied_at` DATETIME(6) NULL,
  `applied_snapshot_id` BIGINT UNSIGNED NULL,
  PRIMARY KEY (`id`),
  KEY `idx_code_patches_run_status` (`run_id`, `status`),
  CONSTRAINT `fk_code_patches_run`
    FOREIGN KEY (`run_id`, `experiment_id`) REFERENCES `experiment_runs` (`id`, `experiment_id`)
    ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_code_patches_workspace`
    FOREIGN KEY (`workspace_id`) REFERENCES `coding_workspaces` (`id`)
    ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_code_patches_student`
    FOREIGN KEY (`student_id`) REFERENCES `users` (`id`)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_code_patches_snapshot`
    FOREIGN KEY (`applied_snapshot_id`) REFERENCES `workspace_snapshots` (`id`)
    ON UPDATE RESTRICT ON DELETE SET NULL,
  CONSTRAINT `ck_code_patches_status` CHECK (`status` IN ('pending', 'applied', 'rejected')),
  CONSTRAINT `ck_code_patches_applied_at` CHECK (`status` <> 'applied' OR `applied_at` IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
