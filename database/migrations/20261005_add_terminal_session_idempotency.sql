-- Persist a client request identity so a lost session-creation response can be retried safely.
-- NULL identities preserve all legacy terminal session rows.
USE `BM-sql`;

SET @client_session_id_column_exists = (
  SELECT COUNT(*)
  FROM `information_schema`.`COLUMNS`
  WHERE `TABLE_SCHEMA` = 'BM-sql'
    AND `TABLE_NAME` = 'terminal_sessions'
    AND `COLUMN_NAME` = 'client_session_id'
);
SET @add_client_session_id_column_sql = IF(
  @client_session_id_column_exists = 0,
  'ALTER TABLE `terminal_sessions` ADD COLUMN `client_session_id` CHAR(36) NULL AFTER `workspace_id`',
  'DO 0'
);
PREPARE add_client_session_id_column FROM @add_client_session_id_column_sql;
EXECUTE add_client_session_id_column;
DEALLOCATE PREPARE add_client_session_id_column;

SET @client_session_id_index_exists = (
  SELECT COUNT(*)
  FROM `information_schema`.`STATISTICS`
  WHERE `TABLE_SCHEMA` = 'BM-sql'
    AND `TABLE_NAME` = 'terminal_sessions'
    AND `INDEX_NAME` = 'uq_terminal_sessions_workspace_client_id'
);
SET @add_client_session_id_index_sql = IF(
  @client_session_id_index_exists = 0,
  'ALTER TABLE `terminal_sessions` ADD UNIQUE KEY `uq_terminal_sessions_workspace_client_id` (`workspace_id`, `client_session_id`)',
  'DO 0'
);
PREPARE add_client_session_id_index FROM @add_client_session_id_index_sql;
EXECUTE add_client_session_id_index;
DEALLOCATE PREPARE add_client_session_id_index;
