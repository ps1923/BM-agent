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
  `password_hash` VARCHAR(255) NULL,
  `role` VARCHAR(20) NOT NULL DEFAULT 'student',
  `status` VARCHAR(20) NOT NULL DEFAULT 'active',
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_users_email` (`email`),
  CONSTRAINT `ck_users_role` CHECK (`role` IN ('student', 'teacher', 'admin')),
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
  `current_node_id` BIGINT UNSIGNED NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'not_started',
  `started_at` DATETIME(6) NULL,
  `last_opened_at` DATETIME(6) NULL,
  `completed_at` DATETIME(6) NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_runs_id_experiment` (`id`, `experiment_id`),
  UNIQUE KEY `uq_runs_experiment_student` (`experiment_id`, `student_id`),
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
  KEY `idx_coding_workspaces_status` (`status`, `last_active_at`),
  CONSTRAINT `fk_coding_workspaces_run`
    FOREIGN KEY (`run_id`) REFERENCES `experiment_runs` (`id`)
    ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `ck_coding_workspaces_status`
    CHECK (`status` IN ('provisioning', 'starting', 'running', 'stopped', 'error', 'expired')),
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
  `shell` VARCHAR(100) NOT NULL,
  `working_directory` VARCHAR(700) NOT NULL DEFAULT '.',
  `runtime_session_id` VARCHAR(255) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'open',
  `started_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `last_active_at` DATETIME(6) NULL,
  `closed_at` DATETIME(6) NULL,
  PRIMARY KEY (`id`),
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
  `command_text` TEXT NOT NULL,
  `working_directory` VARCHAR(700) NOT NULL DEFAULT '.',
  `status` VARCHAR(20) NOT NULL DEFAULT 'queued',
  `exit_code` INT NULL,
  `output_storage_key` VARCHAR(700) NULL,
  `started_at` DATETIME(6) NULL,
  `finished_at` DATETIME(6) NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
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
  `chroma_document_id` VARCHAR(120) NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_bug_cases_student_created` (`student_id`, `created_at`),
  KEY `idx_bug_cases_course_status` (`course_id`, `status`),
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
  CONSTRAINT `ck_bug_cases_vector_status` CHECK (`vector_status` IN ('not_indexed', 'indexed', 'failed'))
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
  `course_id` BIGINT UNSIGNED NOT NULL,
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
