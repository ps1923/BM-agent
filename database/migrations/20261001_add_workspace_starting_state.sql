-- Permit a single request to atomically claim workspace provisioning before Docker starts.
-- Safe to rerun on an existing BM-sql database.
USE `BM-sql`;

SET @workspace_status_check_exists = (
  SELECT COUNT(*)
  FROM `information_schema`.`CHECK_CONSTRAINTS`
  WHERE `CONSTRAINT_SCHEMA` = 'BM-sql'
    AND `CONSTRAINT_NAME` = 'ck_coding_workspaces_status'
);
SET @drop_workspace_status_check_sql = IF(
  @workspace_status_check_exists = 1,
  'ALTER TABLE `coding_workspaces` DROP CHECK `ck_coding_workspaces_status`',
  'DO 0'
);
PREPARE drop_workspace_status_check FROM @drop_workspace_status_check_sql;
EXECUTE drop_workspace_status_check;
DEALLOCATE PREPARE drop_workspace_status_check;

ALTER TABLE `coding_workspaces`
  ADD CONSTRAINT `ck_coding_workspaces_status`
    CHECK (`status` IN ('provisioning', 'starting', 'running', 'stopped', 'error', 'expired'));
