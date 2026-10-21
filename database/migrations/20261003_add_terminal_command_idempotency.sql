-- Add durable client identity to terminal commands without rewriting legacy rows.
-- NULL identities remain valid for historical command_runs records.
USE `BM-sql`;

SET @client_command_id_column_exists = (
  SELECT COUNT(*)
  FROM `information_schema`.`COLUMNS`
  WHERE `TABLE_SCHEMA` = 'BM-sql'
    AND `TABLE_NAME` = 'command_runs'
    AND `COLUMN_NAME` = 'client_command_id'
);
SET @add_client_command_id_column_sql = IF(
  @client_command_id_column_exists = 0,
  'ALTER TABLE `command_runs` ADD COLUMN `client_command_id` CHAR(36) NULL AFTER `terminal_session_id`',
  'DO 0'
);
PREPARE add_client_command_id_column FROM @add_client_command_id_column_sql;
EXECUTE add_client_command_id_column;
DEALLOCATE PREPARE add_client_command_id_column;

SET @client_command_id_index_exists = (
  SELECT COUNT(*)
  FROM `information_schema`.`STATISTICS`
  WHERE `TABLE_SCHEMA` = 'BM-sql'
    AND `TABLE_NAME` = 'command_runs'
    AND `INDEX_NAME` = 'uq_command_runs_session_client_id'
);
SET @add_client_command_id_index_sql = IF(
  @client_command_id_index_exists = 0,
  'ALTER TABLE `command_runs` ADD UNIQUE KEY `uq_command_runs_session_client_id` (`terminal_session_id`, `client_command_id`)',
  'DO 0'
);
PREPARE add_client_command_id_index FROM @add_client_command_id_index_sql;
EXECUTE add_client_command_id_index;
DEALLOCATE PREPARE add_client_command_id_index;
