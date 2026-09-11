-- One-time migration for the first BM-sql deployment review hardening.
-- The canonical fresh-install definitions are also present in database/schema.sql.

USE `BM-sql`;

ALTER TABLE `experiments`
  ADD CONSTRAINT `ck_experiments_published_at`
    CHECK (`status` <> 'published' OR `published_at` IS NOT NULL);

ALTER TABLE `experiment_runs`
  ADD CONSTRAINT `ck_runs_time_order`
    CHECK (`completed_at` IS NULL OR (`started_at` IS NOT NULL AND `completed_at` >= `started_at`)),
  ADD CONSTRAINT `ck_runs_completed_at`
    CHECK (`status` <> 'completed' OR `completed_at` IS NOT NULL);

ALTER TABLE `node_progress`
  ADD CONSTRAINT `ck_node_progress_time_order`
    CHECK (`completed_at` IS NULL OR (`started_at` IS NOT NULL AND `completed_at` >= `started_at`)),
  ADD CONSTRAINT `ck_node_progress_completed`
    CHECK (`status` <> 'completed' OR (`progress_percent` = 100 AND `completed_at` IS NOT NULL));

ALTER TABLE `task_progress`
  ADD CONSTRAINT `ck_task_progress_completed`
    CHECK (`status` <> 'completed' OR `completed_at` IS NOT NULL);

ALTER TABLE `coding_workspaces`
  ADD UNIQUE KEY `uq_coding_workspaces_storage_path` (`storage_path`),
  ADD UNIQUE KEY `uq_coding_workspaces_runtime_instance` (`runtime_instance_id`),
  ADD CONSTRAINT `ck_coding_workspaces_expired_at`
    CHECK (`status` <> 'expired' OR `expires_at` IS NOT NULL);

ALTER TABLE `workspace_files`
  MODIFY `relative_path` VARCHAR(700) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_cs NOT NULL,
  ROW_FORMAT=DYNAMIC,
  ADD CONSTRAINT `ck_workspace_files_entry` CHECK (`is_entry` IN (0, 1)),
  ADD CONSTRAINT `ck_workspace_files_relative_path`
    CHECK (`relative_path` <> ''
      AND `relative_path` NOT REGEXP '^[A-Za-z]:'
      AND `relative_path` NOT REGEXP '^[\\\\/]'
      AND `relative_path` NOT REGEXP '(^|[\\\\/])\\.\\.([\\\\/]|$)');

ALTER TABLE `workspace_snapshots`
  MODIFY `storage_key` VARCHAR(700) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_cs NOT NULL,
  ROW_FORMAT=DYNAMIC;

ALTER TABLE `terminal_sessions`
  ADD CONSTRAINT `ck_terminal_sessions_closed_at`
    CHECK (`status` = 'open' OR `closed_at` IS NOT NULL);

ALTER TABLE `command_runs`
  ADD CONSTRAINT `ck_command_runs_time_order`
    CHECK (`finished_at` IS NULL OR (`started_at` IS NOT NULL AND `finished_at` >= `started_at`)),
  ADD CONSTRAINT `ck_command_runs_finished`
    CHECK (`status` NOT IN ('succeeded', 'failed', 'cancelled') OR `finished_at` IS NOT NULL),
  ADD CONSTRAINT `ck_command_runs_success_exit`
    CHECK (`status` <> 'succeeded' OR `exit_code` = 0);
