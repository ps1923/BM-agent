-- BM-sql learning platform schema
-- Safe to rerun against a compatible schema: no database or table is dropped.

CREATE DATABASE IF NOT EXISTS `BM-sql`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `BM-sql`;

CREATE TABLE IF NOT EXISTS `users` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `email` VARCHAR(320) NOT NULL,
  `display_name` VARCHAR(100) NOT NULL,
  `git_author_name` VARCHAR(100) NULL,
  `git_author_email` VARCHAR(320) NULL,
  `password_hash` VARCHAR(255) NULL,
  `role` VARCHAR(20) NOT NULL DEFAULT 'student',
  `status` VARCHAR(20) NOT NULL DEFAULT 'active',
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_users_email` (`email`),
  CONSTRAINT `ck_users_role` CHECK (`role` IN ('student', 'teacher')),
  CONSTRAINT `ck_users_status` CHECK (`status` IN ('active', 'disabled'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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

CREATE TABLE IF NOT EXISTS `experiment_folders` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `owner_id` BIGINT UNSIGNED NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `description` VARCHAR(255) NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_folders_owner_name` (`owner_id`, `name`),
  UNIQUE KEY `uq_folders_id_owner` (`id`, `owner_id`),
  CONSTRAINT `fk_folders_owner`
    FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`)
    ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `experiments` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `creator_id` BIGINT UNSIGNED NOT NULL,
  `folder_id` BIGINT UNSIGNED NULL,
  `name` VARCHAR(150) NOT NULL,
  `learning_goal` TEXT NOT NULL,
  `description` TEXT NULL,
  `difficulty` VARCHAR(20) NOT NULL,
  `duration_days` SMALLINT UNSIGNED NOT NULL,
  `duration_minutes` INT UNSIGNED NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'draft',
  `visibility` VARCHAR(20) NOT NULL DEFAULT 'private',
  `generation_config` JSON NULL,
  `canvas_offset_x` DECIMAL(12,3) NOT NULL DEFAULT 0,
  `canvas_offset_y` DECIMAL(12,3) NOT NULL DEFAULT 0,
  `canvas_zoom` DECIMAL(6,3) NOT NULL DEFAULT 1,
  `published_at` DATETIME(6) NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_experiments_creator_status` (`creator_id`, `status`),
  KEY `idx_experiments_folder_creator` (`folder_id`, `creator_id`),
  CONSTRAINT `fk_experiments_creator`
    FOREIGN KEY (`creator_id`) REFERENCES `users` (`id`)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_experiments_folder_owner`
    FOREIGN KEY (`folder_id`, `creator_id`)
    REFERENCES `experiment_folders` (`id`, `owner_id`)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_experiments_difficulty`
    CHECK (`difficulty` IN ('beginner', 'easy', 'normal', 'hard', 'challenge')),
  CONSTRAINT `ck_experiments_duration` CHECK (`duration_days` > 0),
  CONSTRAINT `ck_experiments_duration_minutes` CHECK (`duration_minutes` > 0),
  CONSTRAINT `ck_experiments_status`
    CHECK (`status` IN ('draft', 'generating', 'published', 'archived', 'failed')),
  CONSTRAINT `ck_experiments_visibility`
    CHECK (`visibility` IN ('private', 'unlisted', 'public')),
  CONSTRAINT `ck_experiments_zoom` CHECK (`canvas_zoom` > 0),
  CONSTRAINT `ck_experiments_published_at`
    CHECK (`status` <> 'published' OR `published_at` IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `courses` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `teacher_id` BIGINT UNSIGNED NOT NULL,
  `name` VARCHAR(150) NOT NULL,
  `invite_code` VARCHAR(16) NOT NULL,
  `git_repository_url` VARCHAR(1000) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'active',
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_courses_invite_code` (`invite_code`),
  KEY `idx_courses_teacher_status` (`teacher_id`, `status`),
  CONSTRAINT `fk_courses_teacher`
    FOREIGN KEY (`teacher_id`) REFERENCES `users` (`id`)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_courses_status` CHECK (`status` IN ('active', 'archived'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `course_members` (
  `course_id` BIGINT UNSIGNED NOT NULL,
  `student_id` BIGINT UNSIGNED NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'active',
  `joined_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`course_id`, `student_id`),
  KEY `idx_course_members_student_status` (`student_id`, `status`),
  CONSTRAINT `fk_course_members_course`
    FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`)
    ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_course_members_student`
    FOREIGN KEY (`student_id`) REFERENCES `users` (`id`)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_course_members_status` CHECK (`status` IN ('active', 'left'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `course_experiment_assignments` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `course_id` BIGINT UNSIGNED NOT NULL,
  `experiment_id` BIGINT UNSIGNED NOT NULL,
  `publisher_id` BIGINT UNSIGNED NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'published',
  `published_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_course_experiment_assignment` (`course_id`, `experiment_id`),
  KEY `idx_assignments_course_status` (`course_id`, `status`),
  CONSTRAINT `fk_assignments_course`
    FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`)
    ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_assignments_experiment`
    FOREIGN KEY (`experiment_id`) REFERENCES `experiments` (`id`)
    ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_assignments_publisher`
    FOREIGN KEY (`publisher_id`) REFERENCES `users` (`id`)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_assignments_status` CHECK (`status` IN ('published', 'archived'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `experiment_creation_sessions` (
  `id` CHAR(36) NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `status` VARCHAR(30) NOT NULL DEFAULT 'direction_discussion',
  `direction_envelope` JSON NULL,
  `confirmed_intent` JSON NULL,
  `intent_version` INT UNSIGNED NOT NULL DEFAULT 0,
  `intent_hash` CHAR(64) NULL,
  `duration_minutes` INT UNSIGNED NULL,
  `difficulty` VARCHAR(20) NULL,
  `materialization_key` VARCHAR(100) NULL,
  `created_experiment_id` BIGINT UNSIGNED NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_creation_materialization_key` (`materialization_key`),
  KEY `idx_creation_sessions_user_updated` (`user_id`, `updated_at`),
  KEY `idx_creation_sessions_experiment` (`created_experiment_id`),
  CONSTRAINT `fk_creation_sessions_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_creation_sessions_experiment`
    FOREIGN KEY (`created_experiment_id`) REFERENCES `experiments` (`id`)
    ON UPDATE RESTRICT ON DELETE SET NULL,
  CONSTRAINT `ck_creation_sessions_status`
    CHECK (`status` IN ('direction_discussion', 'direction_ready', 'direction_confirmed',
      'configured', 'planning', 'plan_ready', 'materialized', 'failed')),
  CONSTRAINT `ck_creation_sessions_difficulty`
    CHECK (`difficulty` IS NULL OR `difficulty` IN ('beginner', 'easy', 'normal', 'hard', 'challenge')),
  CONSTRAINT `ck_creation_sessions_duration`
    CHECK (`duration_minutes` IS NULL OR `duration_minutes` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `experiment_creation_messages` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `session_id` CHAR(36) NOT NULL,
  `role` VARCHAR(20) NOT NULL,
  `content` TEXT NOT NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_creation_messages_session_created` (`session_id`, `created_at`),
  CONSTRAINT `fk_creation_messages_session`
    FOREIGN KEY (`session_id`) REFERENCES `experiment_creation_sessions` (`id`)
    ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `ck_creation_messages_role`
    CHECK (`role` IN ('user', 'assistant'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `experiment_plan_drafts` (
  `id` CHAR(36) NOT NULL,
  `session_id` CHAR(36) NOT NULL,
  `version` INT UNSIGNED NOT NULL,
  `intent_hash` CHAR(64) NOT NULL,
  `plan_json` JSON NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ready',
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_plan_drafts_session_version` (`session_id`, `version`),
  CONSTRAINT `fk_plan_drafts_session`
    FOREIGN KEY (`session_id`) REFERENCES `experiment_creation_sessions` (`id`)
    ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `ck_plan_drafts_status`
    CHECK (`status` IN ('ready', 'accepted', 'superseded', 'invalid'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `experiment_stages` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `experiment_id` BIGINT UNSIGNED NOT NULL,
  `name` VARCHAR(150) NOT NULL,
  `description` TEXT NULL,
  `sort_order` INT NOT NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_stages_experiment_order` (`experiment_id`, `sort_order`),
  UNIQUE KEY `uq_stages_id_experiment` (`id`, `experiment_id`),
  CONSTRAINT `fk_stages_experiment`
    FOREIGN KEY (`experiment_id`) REFERENCES `experiments` (`id`)
    ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `experiment_nodes` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `experiment_id` BIGINT UNSIGNED NOT NULL,
  `stage_id` BIGINT UNSIGNED NULL,
  `parent_node_id` BIGINT UNSIGNED NULL,
  `sequence_code` VARCHAR(30) NOT NULL,
  `name` VARCHAR(150) NOT NULL,
  `description` TEXT NULL,
  `estimated_minutes` SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  `canvas_x` DECIMAL(12,3) NOT NULL DEFAULT 0,
  `canvas_y` DECIMAL(12,3) NOT NULL DEFAULT 0,
  `sort_order` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_nodes_experiment_sequence` (`experiment_id`, `sequence_code`),
  UNIQUE KEY `uq_nodes_id_experiment` (`id`, `experiment_id`),
  KEY `idx_nodes_experiment_order` (`experiment_id`, `sort_order`),
  KEY `idx_nodes_stage_experiment` (`stage_id`, `experiment_id`),
  KEY `idx_nodes_parent_experiment` (`parent_node_id`, `experiment_id`),
  CONSTRAINT `fk_nodes_experiment`
    FOREIGN KEY (`experiment_id`) REFERENCES `experiments` (`id`)
    ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_nodes_stage`
    FOREIGN KEY (`stage_id`, `experiment_id`)
    REFERENCES `experiment_stages` (`id`, `experiment_id`)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_nodes_parent`
    FOREIGN KEY (`parent_node_id`, `experiment_id`)
    REFERENCES `experiment_nodes` (`id`, `experiment_id`)
    ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `experiment_node_acceptance_criteria` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `experiment_id` BIGINT UNSIGNED NOT NULL,
  `node_id` BIGINT UNSIGNED NOT NULL,
  `version` INT UNSIGNED NOT NULL,
  `source` VARCHAR(20) NOT NULL,
  `criteria_json` JSON NOT NULL,
  `updated_by` BIGINT UNSIGNED NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_node_acceptance_criteria_version` (`node_id`, `version`),
  KEY `idx_node_acceptance_criteria_current` (`experiment_id`, `node_id`, `version`),
  CONSTRAINT `fk_node_acceptance_criteria_node` FOREIGN KEY (`node_id`, `experiment_id`)
    REFERENCES `experiment_nodes` (`id`, `experiment_id`) ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_node_acceptance_criteria_editor` FOREIGN KEY (`updated_by`)
    REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE SET NULL,
  CONSTRAINT `ck_node_acceptance_criteria_source` CHECK (`source` IN ('generated', 'teacher'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `node_dependencies` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `experiment_id` BIGINT UNSIGNED NOT NULL,
  `from_node_id` BIGINT UNSIGNED NOT NULL,
  `to_node_id` BIGINT UNSIGNED NOT NULL,
  `relation_type` VARCHAR(30) NOT NULL DEFAULT 'prerequisite',
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_node_dependencies_edge`
    (`experiment_id`, `from_node_id`, `to_node_id`, `relation_type`),
  KEY `idx_node_dependencies_from` (`from_node_id`, `experiment_id`),
  KEY `idx_node_dependencies_to` (`to_node_id`, `experiment_id`),
  CONSTRAINT `fk_dependencies_from_node`
    FOREIGN KEY (`from_node_id`, `experiment_id`)
    REFERENCES `experiment_nodes` (`id`, `experiment_id`)
    ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_dependencies_to_node`
    FOREIGN KEY (`to_node_id`, `experiment_id`)
    REFERENCES `experiment_nodes` (`id`, `experiment_id`)
    ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `ck_dependencies_distinct_nodes` CHECK (`from_node_id` <> `to_node_id`),
  CONSTRAINT `ck_dependencies_type` CHECK (`relation_type` IN ('prerequisite'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `node_tasks` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `experiment_id` BIGINT UNSIGNED NOT NULL,
  `node_id` BIGINT UNSIGNED NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `description` TEXT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `validation_type` VARCHAR(30) NOT NULL DEFAULT 'manual',
  `validation_config` JSON NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_tasks_id_experiment` (`id`, `experiment_id`),
  UNIQUE KEY `uq_tasks_node_order` (`node_id`, `sort_order`),
  KEY `idx_tasks_node_experiment` (`node_id`, `experiment_id`),
  CONSTRAINT `fk_tasks_node`
    FOREIGN KEY (`node_id`, `experiment_id`)
    REFERENCES `experiment_nodes` (`id`, `experiment_id`)
    ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `ck_tasks_validation_type`
    CHECK (`validation_type` IN ('manual', 'command', 'test', 'agent'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `experiment_runs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `experiment_id` BIGINT UNSIGNED NOT NULL,
  `student_id` BIGINT UNSIGNED NOT NULL,
  `execution_mode` VARCHAR(20) NOT NULL DEFAULT 'legacy',
  `current_node_id` BIGINT UNSIGNED NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'not_started',
  `started_at` DATETIME(6) NULL,
  `last_opened_at` DATETIME(6) NULL,
  `completed_at` DATETIME(6) NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_runs_id_experiment` (`id`, `experiment_id`),
  UNIQUE KEY `uq_runs_experiment_student_mode` (`experiment_id`, `student_id`, `execution_mode`),
  KEY `idx_runs_student_recent` (`student_id`, `last_opened_at`),
  KEY `idx_runs_current_node` (`current_node_id`, `experiment_id`),
  CONSTRAINT `fk_runs_experiment`
    FOREIGN KEY (`experiment_id`) REFERENCES `experiments` (`id`)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_runs_student`
    FOREIGN KEY (`student_id`) REFERENCES `users` (`id`)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_runs_current_node`
    FOREIGN KEY (`current_node_id`, `experiment_id`)
    REFERENCES `experiment_nodes` (`id`, `experiment_id`)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_runs_status`
    CHECK (`status` IN ('not_started', 'in_progress', 'paused', 'completed')),
  CONSTRAINT `ck_runs_time_order`
    CHECK (`completed_at` IS NULL OR (`started_at` IS NOT NULL AND `completed_at` >= `started_at`)),
  CONSTRAINT `ck_runs_completed_at`
    CHECK (`status` <> 'completed' OR `completed_at` IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `node_progress` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `experiment_id` BIGINT UNSIGNED NOT NULL,
  `run_id` BIGINT UNSIGNED NOT NULL,
  `node_id` BIGINT UNSIGNED NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'locked',
  `progress_percent` DECIMAL(5,2) NOT NULL DEFAULT 0,
  `started_at` DATETIME(6) NULL,
  `completed_at` DATETIME(6) NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_node_progress_run_node` (`run_id`, `node_id`),
  KEY `idx_node_progress_run_experiment` (`run_id`, `experiment_id`),
  KEY `idx_node_progress_node_experiment` (`node_id`, `experiment_id`),
  CONSTRAINT `fk_node_progress_run`
    FOREIGN KEY (`run_id`, `experiment_id`)
    REFERENCES `experiment_runs` (`id`, `experiment_id`)
    ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_node_progress_node`
    FOREIGN KEY (`node_id`, `experiment_id`)
    REFERENCES `experiment_nodes` (`id`, `experiment_id`)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_node_progress_status`
    CHECK (`status` IN ('locked', 'pending', 'in_progress', 'completed')),
  CONSTRAINT `ck_node_progress_percent`
    CHECK (`progress_percent` >= 0 AND `progress_percent` <= 100),
  CONSTRAINT `ck_node_progress_time_order`
    CHECK (`completed_at` IS NULL OR (`started_at` IS NOT NULL AND `completed_at` >= `started_at`)),
  CONSTRAINT `ck_node_progress_completed`
    CHECK (`status` <> 'completed' OR (`progress_percent` = 100 AND `completed_at` IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `task_progress` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `experiment_id` BIGINT UNSIGNED NOT NULL,
  `run_id` BIGINT UNSIGNED NOT NULL,
  `task_id` BIGINT UNSIGNED NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending',
  `submission_metadata` JSON NULL,
  `validation_result` JSON NULL,
  `completed_at` DATETIME(6) NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_task_progress_run_task` (`run_id`, `task_id`),
  KEY `idx_task_progress_run_experiment` (`run_id`, `experiment_id`),
  KEY `idx_task_progress_task_experiment` (`task_id`, `experiment_id`),
  CONSTRAINT `fk_task_progress_run`
    FOREIGN KEY (`run_id`, `experiment_id`)
    REFERENCES `experiment_runs` (`id`, `experiment_id`)
    ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_task_progress_task`
    FOREIGN KEY (`task_id`, `experiment_id`)
    REFERENCES `node_tasks` (`id`, `experiment_id`)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_task_progress_status`
    CHECK (`status` IN ('pending', 'in_progress', 'completed', 'failed')),
  CONSTRAINT `ck_task_progress_completed`
    CHECK (`status` <> 'completed' OR `completed_at` IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `coding_workspaces` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `run_id` BIGINT UNSIGNED NOT NULL,
  `storage_path` VARCHAR(700) NOT NULL,
  `runtime_image` VARCHAR(255) NOT NULL,
  `runtime_instance_id` VARCHAR(255) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'provisioning',
  `workspace_type` VARCHAR(20) NOT NULL DEFAULT 'server',
  `local_workspace_id` CHAR(36) NULL,
  `cpu_limit` DECIMAL(6,3) NULL,
  `memory_limit_mb` INT UNSIGNED NULL,
  `expires_at` DATETIME(6) NULL,
  `last_active_at` DATETIME(6) NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_coding_workspaces_run` (`run_id`),
  UNIQUE KEY `uq_coding_workspaces_storage_path` (`storage_path`),
  UNIQUE KEY `uq_coding_workspaces_runtime_instance` (`runtime_instance_id`),
  UNIQUE KEY `uq_coding_workspaces_local_workspace_id` (`local_workspace_id`),
  KEY `idx_coding_workspaces_status` (`status`, `last_active_at`),
  CONSTRAINT `fk_coding_workspaces_run`
    FOREIGN KEY (`run_id`) REFERENCES `experiment_runs` (`id`)
    ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `ck_coding_workspaces_status`
    CHECK (`status` IN ('provisioning', 'starting', 'running', 'stopped', 'error', 'expired')),
  CONSTRAINT `ck_coding_workspaces_type`
    CHECK (`workspace_type` IN ('server', 'desktop')),
  CONSTRAINT `ck_coding_workspaces_cpu`
    CHECK (`cpu_limit` IS NULL OR `cpu_limit` > 0),
  CONSTRAINT `ck_coding_workspaces_memory`
    CHECK (`memory_limit_mb` IS NULL OR `memory_limit_mb` > 0),
  CONSTRAINT `ck_coding_workspaces_expired_at`
    CHECK (`status` <> 'expired' OR `expires_at` IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `workspace_files` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `workspace_id` BIGINT UNSIGNED NOT NULL,
  `relative_path` VARCHAR(700) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_cs NOT NULL,
  `display_name` VARCHAR(255) NOT NULL,
  `language` VARCHAR(50) NULL,
  `size_bytes` BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `content_hash` CHAR(64) NULL,
  `is_entry` BOOLEAN NOT NULL DEFAULT FALSE,
  `sort_order` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_workspace_files_path` (`workspace_id`, `relative_path`),
  KEY `idx_workspace_files_order` (`workspace_id`, `sort_order`),
  CONSTRAINT `fk_workspace_files_workspace`
    FOREIGN KEY (`workspace_id`) REFERENCES `coding_workspaces` (`id`)
    ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `ck_workspace_files_size` CHECK (`size_bytes` >= 0),
  CONSTRAINT `ck_workspace_files_entry` CHECK (`is_entry` IN (0, 1)),
  CONSTRAINT `ck_workspace_files_relative_path`
    CHECK (`relative_path` <> ''
      AND `relative_path` NOT REGEXP '^[A-Za-z]:'
      AND `relative_path` NOT REGEXP '^[\\\\/]'
      AND `relative_path` NOT REGEXP '(^|[\\\\/])\\.\\.([\\\\/]|$)')
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `workspace_snapshots` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `workspace_id` BIGINT UNSIGNED NOT NULL,
  `node_id` BIGINT UNSIGNED NULL,
  `storage_key` VARCHAR(700) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_cs NOT NULL,
  `snapshot_type` VARCHAR(30) NOT NULL DEFAULT 'manual',
  `description` VARCHAR(500) NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_workspace_snapshots_key` (`workspace_id`, `storage_key`),
  UNIQUE KEY `uq_workspace_snapshots_node_completion` (`workspace_id`, `node_id`, `snapshot_type`),
  KEY `idx_workspace_snapshots_created` (`workspace_id`, `created_at`),
  CONSTRAINT `fk_workspace_snapshots_workspace`
    FOREIGN KEY (`workspace_id`) REFERENCES `coding_workspaces` (`id`)
    ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_workspace_snapshots_node`
    FOREIGN KEY (`node_id`) REFERENCES `experiment_nodes` (`id`)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_workspace_snapshots_type`
    CHECK (`snapshot_type` IN ('manual', 'node_completion', 'autosave', 'system'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `terminal_sessions` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `workspace_id` BIGINT UNSIGNED NOT NULL,
  `client_session_id` CHAR(36) NULL,
  `shell` VARCHAR(100) NOT NULL,
  `working_directory` VARCHAR(700) NOT NULL DEFAULT '.',
  `runtime_session_id` VARCHAR(255) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'open',
  `started_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `last_active_at` DATETIME(6) NULL,
  `closed_at` DATETIME(6) NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_terminal_sessions_workspace_client_id` (`workspace_id`, `client_session_id`),
  KEY `idx_terminal_sessions_workspace_status` (`workspace_id`, `status`),
  CONSTRAINT `fk_terminal_sessions_workspace`
    FOREIGN KEY (`workspace_id`) REFERENCES `coding_workspaces` (`id`)
    ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `ck_terminal_sessions_status`
    CHECK (`status` IN ('open', 'closed', 'expired', 'error')),
  CONSTRAINT `ck_terminal_sessions_closed_at`
    CHECK (`status` = 'open' OR `closed_at` IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `command_runs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `terminal_session_id` BIGINT UNSIGNED NOT NULL,
  `client_command_id` CHAR(36) NULL,
  `command_text` TEXT NOT NULL,
  `working_directory` VARCHAR(700) NOT NULL DEFAULT '.',
  `status` VARCHAR(20) NOT NULL DEFAULT 'queued',
  `exit_code` INT NULL,
  `output_storage_key` VARCHAR(700) NULL,
  `started_at` DATETIME(6) NULL,
  `finished_at` DATETIME(6) NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_command_runs_session_client_id` (`terminal_session_id`, `client_command_id`),
  KEY `idx_command_runs_session_created` (`terminal_session_id`, `created_at`),
  CONSTRAINT `fk_command_runs_terminal`
    FOREIGN KEY (`terminal_session_id`) REFERENCES `terminal_sessions` (`id`)
    ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `ck_command_runs_status`
    CHECK (`status` IN ('queued', 'running', 'succeeded', 'failed', 'cancelled')),
  CONSTRAINT `ck_command_runs_time_order`
    CHECK (`finished_at` IS NULL OR (`started_at` IS NOT NULL AND `finished_at` >= `started_at`)),
  CONSTRAINT `ck_command_runs_finished`
    CHECK (`status` NOT IN ('succeeded', 'failed', 'cancelled') OR `finished_at` IS NOT NULL),
  CONSTRAINT `ck_command_runs_success_exit`
    CHECK (`status` <> 'succeeded' OR `exit_code` = 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `chat_messages` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `experiment_id` BIGINT UNSIGNED NOT NULL,
  `run_id` BIGINT UNSIGNED NOT NULL,
  `node_id` BIGINT UNSIGNED NULL,
  `role` VARCHAR(20) NOT NULL,
  `message_type` VARCHAR(30) NOT NULL DEFAULT 'normal',
  `content` MEDIUMTEXT NOT NULL,
  `metadata` JSON NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_chat_messages_run_created` (`run_id`, `created_at`),
  KEY `idx_chat_messages_run_experiment` (`run_id`, `experiment_id`),
  KEY `idx_chat_messages_node_experiment` (`node_id`, `experiment_id`),
  CONSTRAINT `fk_chat_messages_run`
    FOREIGN KEY (`run_id`, `experiment_id`)
    REFERENCES `experiment_runs` (`id`, `experiment_id`)
    ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_chat_messages_node`
    FOREIGN KEY (`node_id`, `experiment_id`)
    REFERENCES `experiment_nodes` (`id`, `experiment_id`)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_chat_messages_role`
    CHECK (`role` IN ('user', 'assistant', 'system', 'tool')),
  CONSTRAINT `ck_chat_messages_type`
    CHECK (`message_type` IN ('normal', 'soften', 'hint', 'event'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `bug_cases` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `experiment_id` BIGINT UNSIGNED NOT NULL,
  `run_id` BIGINT UNSIGNED NOT NULL,
  `node_id` BIGINT UNSIGNED NULL,
  `course_id` BIGINT UNSIGNED NULL,
  `student_id` BIGINT UNSIGNED NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `problem` MEDIUMTEXT NOT NULL,
  `solution` MEDIUMTEXT NULL,
  `technology_stack` VARCHAR(100) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending',
  `visibility` VARCHAR(20) NOT NULL DEFAULT 'personal',
  `vector_status` VARCHAR(20) NOT NULL DEFAULT 'not_indexed',
  `vector_index_attempts` SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  `vector_index_retry_at` DATETIME(6) NULL,
  `chroma_document_id` VARCHAR(120) NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_bug_cases_student_created` (`student_id`, `created_at`),
  KEY `idx_bug_cases_course_status` (`course_id`, `status`),
  KEY `idx_bug_cases_vector_retry` (`status`, `vector_status`, `vector_index_retry_at`),
  CONSTRAINT `fk_bug_cases_run`
    FOREIGN KEY (`run_id`, `experiment_id`) REFERENCES `experiment_runs` (`id`, `experiment_id`)
    ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_bug_cases_node`
    FOREIGN KEY (`node_id`, `experiment_id`) REFERENCES `experiment_nodes` (`id`, `experiment_id`)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_bug_cases_course`
    FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`)
    ON UPDATE RESTRICT ON DELETE SET NULL,
  CONSTRAINT `fk_bug_cases_student`
    FOREIGN KEY (`student_id`) REFERENCES `users` (`id`)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_bug_cases_status` CHECK (`status` IN ('pending', 'approved', 'rejected')),
  CONSTRAINT `ck_bug_cases_visibility` CHECK (`visibility` IN ('personal', 'course')),
  CONSTRAINT `ck_bug_cases_vector_status` CHECK (`vector_status` IN ('not_indexed', 'indexed', 'failed')),
  CONSTRAINT `ck_bug_cases_vector_attempts` CHECK (`vector_index_attempts` <= 6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `bug_case_reviews` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `bug_case_id` BIGINT UNSIGNED NOT NULL,
  `teacher_id` BIGINT UNSIGNED NOT NULL,
  `decision` VARCHAR(20) NOT NULL,
  `feedback` VARCHAR(1000) NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_bug_case_reviews_case_created` (`bug_case_id`, `created_at`),
  CONSTRAINT `fk_bug_case_reviews_case`
    FOREIGN KEY (`bug_case_id`) REFERENCES `bug_cases` (`id`)
    ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_bug_case_reviews_teacher`
    FOREIGN KEY (`teacher_id`) REFERENCES `users` (`id`)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_bug_case_reviews_decision` CHECK (`decision` IN ('approved', 'rejected'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `experiment_reviewers` (
  `experiment_id` BIGINT UNSIGNED NOT NULL,
  `course_id` BIGINT UNSIGNED NULL COMMENT 'NULL indicates an untrusted legacy assignment that cannot grant access',
  `teacher_id` BIGINT UNSIGNED NOT NULL,
  `assigned_by_teacher_id` BIGINT UNSIGNED NOT NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`experiment_id`),
  KEY `idx_experiment_reviewers_teacher` (`teacher_id`, `experiment_id`),
  CONSTRAINT `fk_experiment_reviewers_experiment` FOREIGN KEY (`experiment_id`) REFERENCES `experiments` (`id`) ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_experiment_reviewers_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_experiment_reviewers_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_experiment_reviewers_assigned_by_teacher` FOREIGN KEY (`assigned_by_teacher_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
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

-- Git-only student runs and immutable commit-based acceptance. Legacy workspace
-- and DSH snapshot tables remain available for historical compatibility.
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
  `criteria_version` INT UNSIGNED NULL,
  `criteria_source` VARCHAR(20) NULL,
  `criteria_snapshot_json` JSON NULL,
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
  `criteria_version` INT UNSIGNED NULL,
  `criteria_source` VARCHAR(20) NULL,
  `criteria_snapshot_json` JSON NULL,
  `criterion_results_json` JSON NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`submission_id`, `node_id`),
  CONSTRAINT `fk_git_acceptance_results_node` FOREIGN KEY (`submission_id`, `node_id`) REFERENCES `git_submission_nodes` (`submission_id`, `node_id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_git_acceptance_decision` CHECK (`decision` IN ('meets', 'partially_meets', 'does_not_meet', 'unable_to_verify')),
  CONSTRAINT `ck_git_acceptance_execution` CHECK (`execution_check` IN ('not_run', 'not_safe', 'not_configured'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
  CONSTRAINT `fk_diagnosis_tasks_run` FOREIGN KEY (`run_id`, `experiment_id`)
    REFERENCES `experiment_runs` (`id`, `experiment_id`) ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_diagnosis_tasks_workspace` FOREIGN KEY (`workspace_id`) REFERENCES `coding_workspaces` (`id`)
    ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_diagnosis_tasks_student` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_diagnosis_tasks_status` CHECK (`status` IN ('queued', 'running', 'completed', 'failed', 'cancelled'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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

CREATE TABLE IF NOT EXISTS `teacher_reviews` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `course_id` BIGINT UNSIGNED NULL,
  `experiment_id` BIGINT UNSIGNED NOT NULL,
  `run_id` BIGINT UNSIGNED NOT NULL,
  `teacher_id` BIGINT UNSIGNED NOT NULL,
  `student_id` BIGINT UNSIGNED NOT NULL,
  `rating` TINYINT UNSIGNED NULL,
  `feedback` VARCHAR(2000) NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_teacher_reviews_run_teacher` (`run_id`, `teacher_id`),
  KEY `idx_teacher_reviews_course_updated` (`course_id`, `updated_at`),
  CONSTRAINT `fk_teacher_reviews_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`) ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_teacher_reviews_run` FOREIGN KEY (`run_id`, `experiment_id`) REFERENCES `experiment_runs` (`id`, `experiment_id`) ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_teacher_reviews_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_teacher_reviews_student` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_teacher_reviews_rating` CHECK (`rating` IS NULL OR (`rating` >= 0 AND `rating` <= 100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
