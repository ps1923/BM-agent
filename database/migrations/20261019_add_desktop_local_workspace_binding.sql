ALTER TABLE `coding_workspaces`
  ADD COLUMN `workspace_type` VARCHAR(20) NOT NULL DEFAULT 'server' AFTER `status`,
  ADD COLUMN `local_workspace_id` CHAR(36) NULL AFTER `workspace_type`,
  ADD UNIQUE KEY `uq_coding_workspaces_local_workspace_id` (`local_workspace_id`),
  ADD CONSTRAINT `ck_coding_workspaces_type`
    CHECK (`workspace_type` IN ('server', 'desktop'));

-- desktop://<random UUID> is only an opaque compatibility marker for legacy NOT NULL columns.
-- No client filesystem path is ever stored by this migration or the desktop-run creation endpoint.
