-- Additive Git/Gitea commit workflow. Existing workspace, DSH and snapshot rows
-- are retained. Safe to rerun on MySQL 8.0.16+.
USE `BM-sql`;

CREATE TABLE IF NOT EXISTS `git_repositories` (
  `run_id` BIGINT UNSIGNED NOT NULL,
  `experiment_id` BIGINT UNSIGNED NOT NULL,
  `student_id` BIGINT UNSIGNED NOT NULL,
  `gitea_repo_id` BIGINT UNSIGNED NULL,
  `gitea_owner` VARCHAR(100) NOT NULL,
  `repo_name` VARCHAR(100) NOT NULL,
  `repo_url` VARCHAR(1000) NOT NULL,
  `clone_url` VARCHAR(1000) NOT NULL,
  `branch_name` VARCHAR(100) NOT NULL DEFAULT 'main',
  `credential_username` VARCHAR(100) NOT NULL,
  `credential_ciphertext` TEXT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'provisioning',
  `last_error_code` VARCHAR(80) NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`run_id`),
  UNIQUE KEY `uq_git_repositories_gitea_repo_id` (`gitea_repo_id`),
  UNIQUE KEY `uq_git_repositories_owner_name` (`gitea_owner`, `repo_name`),
  KEY `idx_git_repositories_student` (`student_id`, `experiment_id`),
  CONSTRAINT `fk_git_repositories_run` FOREIGN KEY (`run_id`, `experiment_id`) REFERENCES `experiment_runs` (`id`, `experiment_id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_git_repositories_student` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_git_repositories_status` CHECK (`status` IN ('provisioning', 'ready', 'error'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `git_submissions` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `run_id` BIGINT UNSIGNED NOT NULL,
  `experiment_id` BIGINT UNSIGNED NOT NULL,
  `student_id` BIGINT UNSIGNED NOT NULL,
  `commit_sha` CHAR(64) NOT NULL,
  `parent_sha` CHAR(64) NULL,
  `commit_message` VARCHAR(1000) NULL,
  `commit_author` VARCHAR(200) NULL,
  `committed_at` DATETIME(6) NULL,
  `description` VARCHAR(500) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'queued',
  `webhook_seen_at` DATETIME(6) NULL,
  `error_code` VARCHAR(80) NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_git_submissions_run_sha` (`run_id`, `commit_sha`),
  KEY `idx_git_submissions_run_created` (`run_id`, `created_at`, `id`),
  KEY `idx_git_submissions_repo_sha` (`experiment_id`, `commit_sha`),
  CONSTRAINT `fk_git_submissions_run` FOREIGN KEY (`run_id`, `experiment_id`) REFERENCES `experiment_runs` (`id`, `experiment_id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_git_submissions_student` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_git_submissions_status` CHECK (`status` IN ('queued', 'running', 'completed', 'failed'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `git_submission_nodes` (
  `submission_id` BIGINT UNSIGNED NOT NULL,
  `experiment_id` BIGINT UNSIGNED NOT NULL,
  `node_id` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`submission_id`, `node_id`),
  KEY `idx_git_submission_nodes_node` (`experiment_id`, `node_id`, `submission_id`),
  CONSTRAINT `fk_git_submission_nodes_submission` FOREIGN KEY (`submission_id`) REFERENCES `git_submissions` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_git_submission_nodes_node` FOREIGN KEY (`node_id`, `experiment_id`) REFERENCES `experiment_nodes` (`id`, `experiment_id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `git_acceptance_results` (
  `submission_id` BIGINT UNSIGNED NOT NULL,
  `experiment_id` BIGINT UNSIGNED NOT NULL,
  `node_id` BIGINT UNSIGNED NOT NULL,
  `decision` VARCHAR(40) NOT NULL,
  `summary` TEXT NOT NULL,
  `evidence_json` JSON NOT NULL,
  `suggestions_json` JSON NOT NULL,
  `bug_draft_json` JSON NULL,
  `execution_check` VARCHAR(30) NOT NULL DEFAULT 'not_run',
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`submission_id`, `node_id`),
  CONSTRAINT `fk_git_acceptance_results_node` FOREIGN KEY (`submission_id`, `node_id`) REFERENCES `git_submission_nodes` (`submission_id`, `node_id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_git_acceptance_decision` CHECK (`decision` IN ('meets', 'partially_meets', 'does_not_meet', 'unable_to_verify')),
  CONSTRAINT `ck_git_acceptance_execution` CHECK (`execution_check` IN ('not_run', 'not_safe', 'not_configured'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Only Git-mode runs may be normalized. Legacy DSH/workspace progress remains
-- unchanged and read-only for historical compatibility.
UPDATE `node_progress` np
JOIN `experiment_runs` r ON r.id = np.run_id AND r.experiment_id = np.experiment_id
SET np.status = 'pending'
WHERE np.status = 'locked' AND r.execution_mode = 'git';
