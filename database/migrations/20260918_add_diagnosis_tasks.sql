-- BE-10 deep diagnosis task state. Safe to run repeatedly on a compatible BM-sql installation.
USE `BM-sql`;

CREATE TABLE IF NOT EXISTS `diagnosis_tasks` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `run_id` BIGINT UNSIGNED NOT NULL,
  `experiment_id` BIGINT UNSIGNED NOT NULL,
  `workspace_id` BIGINT UNSIGNED NOT NULL,
  `student_id` BIGINT UNSIGNED NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'queued',
  `result_json` MEDIUMTEXT NULL,
  `error_message` VARCHAR(200) NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `started_at` DATETIME(6) NULL,
  `completed_at` DATETIME(6) NULL,
  PRIMARY KEY (`id`),
  KEY `idx_diagnosis_tasks_run_status` (`run_id`, `student_id`, `status`),
  CONSTRAINT `fk_diagnosis_tasks_run`
    FOREIGN KEY (`run_id`, `experiment_id`) REFERENCES `experiment_runs` (`id`, `experiment_id`)
    ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_diagnosis_tasks_workspace` FOREIGN KEY (`workspace_id`) REFERENCES `coding_workspaces` (`id`)
    ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_diagnosis_tasks_student` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_diagnosis_tasks_status` CHECK (`status` IN ('queued', 'running', 'completed', 'failed', 'cancelled'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
