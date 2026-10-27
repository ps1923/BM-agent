-- Add bounded retry state for approved Bug cases whose Chroma indexing failed.
USE `BM-sql`;

SET @vector_attempts_exists = (
  SELECT COUNT(*) FROM `information_schema`.`COLUMNS`
  WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'bug_cases'
    AND `COLUMN_NAME` = 'vector_index_attempts'
);
SET @vector_attempts_sql = IF(@vector_attempts_exists = 0,
  'ALTER TABLE `bug_cases` ADD COLUMN `vector_index_attempts` SMALLINT UNSIGNED NOT NULL DEFAULT 0 AFTER `vector_status`',
  'SELECT 1');
PREPARE vector_attempts_stmt FROM @vector_attempts_sql;
EXECUTE vector_attempts_stmt;
DEALLOCATE PREPARE vector_attempts_stmt;

SET @vector_retry_exists = (
  SELECT COUNT(*) FROM `information_schema`.`COLUMNS`
  WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'bug_cases'
    AND `COLUMN_NAME` = 'vector_index_retry_at'
);
SET @vector_retry_sql = IF(@vector_retry_exists = 0,
  'ALTER TABLE `bug_cases` ADD COLUMN `vector_index_retry_at` DATETIME(6) NULL AFTER `vector_index_attempts`',
  'SELECT 1');
PREPARE vector_retry_stmt FROM @vector_retry_sql;
EXECUTE vector_retry_stmt;
DEALLOCATE PREPARE vector_retry_stmt;

SET @vector_retry_index_exists = (
  SELECT COUNT(*) FROM `information_schema`.`STATISTICS`
  WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'bug_cases'
    AND `INDEX_NAME` = 'idx_bug_cases_vector_retry'
);
SET @vector_retry_index_sql = IF(@vector_retry_index_exists = 0,
  'CREATE INDEX `idx_bug_cases_vector_retry` ON `bug_cases` (`status`, `vector_status`, `vector_index_retry_at`)',
  'SELECT 1');
PREPARE vector_retry_index_stmt FROM @vector_retry_index_sql;
EXECUTE vector_retry_index_stmt;
DEALLOCATE PREPARE vector_retry_index_stmt;

SET @vector_attempts_check_exists = (
  SELECT COUNT(*) FROM `information_schema`.`TABLE_CONSTRAINTS`
  WHERE `CONSTRAINT_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'bug_cases'
    AND `CONSTRAINT_NAME` = 'ck_bug_cases_vector_attempts'
);
SET @vector_attempts_check_sql = IF(@vector_attempts_check_exists = 0,
  'ALTER TABLE `bug_cases` ADD CONSTRAINT `ck_bug_cases_vector_attempts` CHECK (`vector_index_attempts` <= 6)',
  'SELECT 1');
PREPARE vector_attempts_check_stmt FROM @vector_attempts_check_sql;
EXECUTE vector_attempts_check_stmt;
DEALLOCATE PREPARE vector_attempts_check_stmt;
