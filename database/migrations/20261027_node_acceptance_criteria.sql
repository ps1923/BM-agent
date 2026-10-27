-- Adds versioned node-level acceptance criteria while preserving all existing experiment data.
USE `BM-sql`;

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

SET @criteria_version_column_exists = (
  SELECT COUNT(*) FROM `information_schema`.`COLUMNS`
  WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'git_acceptance_results'
    AND `COLUMN_NAME` = 'criteria_version'
);
SET @criteria_version_sql = IF(@criteria_version_column_exists = 0,
  'ALTER TABLE `git_acceptance_results` ADD COLUMN `criteria_version` INT UNSIGNED NULL AFTER `execution_check`',
  'SELECT 1');
PREPARE criteria_version_stmt FROM @criteria_version_sql;
EXECUTE criteria_version_stmt;
DEALLOCATE PREPARE criteria_version_stmt;

SET @criteria_source_column_exists = (
  SELECT COUNT(*) FROM `information_schema`.`COLUMNS`
  WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'git_acceptance_results'
    AND `COLUMN_NAME` = 'criteria_source'
);
SET @criteria_source_sql = IF(@criteria_source_column_exists = 0,
  'ALTER TABLE `git_acceptance_results` ADD COLUMN `criteria_source` VARCHAR(20) NULL AFTER `criteria_version`',
  'SELECT 1');
PREPARE criteria_source_stmt FROM @criteria_source_sql;
EXECUTE criteria_source_stmt;
DEALLOCATE PREPARE criteria_source_stmt;

SET @criteria_snapshot_column_exists = (
  SELECT COUNT(*) FROM `information_schema`.`COLUMNS`
  WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'git_acceptance_results'
    AND `COLUMN_NAME` = 'criteria_snapshot_json'
);
SET @criteria_snapshot_sql = IF(@criteria_snapshot_column_exists = 0,
  'ALTER TABLE `git_acceptance_results` ADD COLUMN `criteria_snapshot_json` JSON NULL AFTER `criteria_source`',
  'SELECT 1');
PREPARE criteria_snapshot_stmt FROM @criteria_snapshot_sql;
EXECUTE criteria_snapshot_stmt;
DEALLOCATE PREPARE criteria_snapshot_stmt;

SET @criterion_results_column_exists = (
  SELECT COUNT(*) FROM `information_schema`.`COLUMNS`
  WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'git_acceptance_results'
    AND `COLUMN_NAME` = 'criterion_results_json'
);
SET @criterion_results_sql = IF(@criterion_results_column_exists = 0,
  'ALTER TABLE `git_acceptance_results` ADD COLUMN `criterion_results_json` JSON NULL AFTER `criteria_snapshot_json`',
  'SELECT 1');
PREPARE criterion_results_stmt FROM @criterion_results_sql;
EXECUTE criterion_results_stmt;
DEALLOCATE PREPARE criterion_results_stmt;

SET @submission_criteria_version_exists = (
  SELECT COUNT(*) FROM `information_schema`.`COLUMNS`
  WHERE `TABLE_SCHEMA` = 'BM-sql' AND `TABLE_NAME` = 'git_submission_nodes'
    AND `COLUMN_NAME` = 'criteria_version'
);
SET @submission_criteria_version_sql = IF(@submission_criteria_version_exists = 0,
  'ALTER TABLE `git_submission_nodes` ADD COLUMN `criteria_version` INT UNSIGNED NULL AFTER `node_id`',
  'SELECT 1');
PREPARE submission_criteria_version_stmt FROM @submission_criteria_version_sql;
EXECUTE submission_criteria_version_stmt;
DEALLOCATE PREPARE submission_criteria_version_stmt;

SET @submission_criteria_source_exists = (
  SELECT COUNT(*) FROM `information_schema`.`COLUMNS`
  WHERE `TABLE_SCHEMA` = 'BM-sql' AND `TABLE_NAME` = 'git_submission_nodes'
    AND `COLUMN_NAME` = 'criteria_source'
);
SET @submission_criteria_source_sql = IF(@submission_criteria_source_exists = 0,
  'ALTER TABLE `git_submission_nodes` ADD COLUMN `criteria_source` VARCHAR(20) NULL AFTER `criteria_version`',
  'SELECT 1');
PREPARE submission_criteria_source_stmt FROM @submission_criteria_source_sql;
EXECUTE submission_criteria_source_stmt;
DEALLOCATE PREPARE submission_criteria_source_stmt;

SET @submission_criteria_snapshot_exists = (
  SELECT COUNT(*) FROM `information_schema`.`COLUMNS`
  WHERE `TABLE_SCHEMA` = 'BM-sql' AND `TABLE_NAME` = 'git_submission_nodes'
    AND `COLUMN_NAME` = 'criteria_snapshot_json'
);
SET @submission_criteria_snapshot_sql = IF(@submission_criteria_snapshot_exists = 0,
  'ALTER TABLE `git_submission_nodes` ADD COLUMN `criteria_snapshot_json` JSON NULL AFTER `criteria_source`',
  'SELECT 1');
PREPARE submission_criteria_snapshot_stmt FROM @submission_criteria_snapshot_sql;
EXECUTE submission_criteria_snapshot_stmt;
DEALLOCATE PREPARE submission_criteria_snapshot_stmt;
