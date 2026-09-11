-- Adds isolated experiment-creation sessions and rooted tree nodes.
-- Safe to rerun on MySQL 8.0.16+: existing columns/constraints are detected.

USE `BM-sql`;

DELIMITER //
DROP PROCEDURE IF EXISTS `bm_add_column_if_missing`//
CREATE PROCEDURE `bm_add_column_if_missing`(
  IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_definition TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'BM-sql' AND TABLE_NAME = p_table AND COLUMN_NAME = p_column
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END//

DROP PROCEDURE IF EXISTS `bm_drop_check_if_exists`//
CREATE PROCEDURE `bm_drop_check_if_exists`(IN p_table VARCHAR(64), IN p_constraint VARCHAR(64))
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = 'BM-sql' AND TABLE_NAME = p_table
      AND CONSTRAINT_NAME = p_constraint AND CONSTRAINT_TYPE = 'CHECK'
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', p_table, '` DROP CHECK `', p_constraint, '`');
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END//

DROP PROCEDURE IF EXISTS `bm_add_constraint_if_missing`//
CREATE PROCEDURE `bm_add_constraint_if_missing`(
  IN p_table VARCHAR(64), IN p_constraint VARCHAR(64), IN p_definition TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = 'BM-sql' AND TABLE_NAME = p_table AND CONSTRAINT_NAME = p_constraint
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD CONSTRAINT `', p_constraint, '` ', p_definition);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END//

DROP PROCEDURE IF EXISTS `bm_add_index_if_missing`//
CREATE PROCEDURE `bm_add_index_if_missing`(
  IN p_table VARCHAR(64), IN p_index VARCHAR(64), IN p_columns TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'BM-sql' AND TABLE_NAME = p_table AND INDEX_NAME = p_index
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD INDEX `', p_index, '` ', p_columns);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END//
DELIMITER ;

CALL `bm_add_column_if_missing`('experiments', 'duration_minutes', 'INT UNSIGNED NULL AFTER `duration_days`');
UPDATE `experiments`
SET `duration_minutes` = GREATEST(1, `duration_days` * 1440)
WHERE `duration_minutes` IS NULL;
ALTER TABLE `experiments` MODIFY `duration_minutes` INT UNSIGNED NOT NULL;
CALL `bm_drop_check_if_exists`('experiments', 'ck_experiments_difficulty');
UPDATE `experiments` SET `difficulty` = 'normal' WHERE `difficulty` = 'intermediate';
CALL `bm_add_constraint_if_missing`(
  'experiments', 'ck_experiments_difficulty',
  'CHECK (`difficulty` IN (''beginner'', ''easy'', ''normal'', ''hard'', ''challenge''))'
);
CALL `bm_add_constraint_if_missing`(
  'experiments', 'ck_experiments_duration_minutes', 'CHECK (`duration_minutes` > 0)'
);

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
  CONSTRAINT `fk_creation_sessions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_creation_sessions_experiment` FOREIGN KEY (`created_experiment_id`) REFERENCES `experiments` (`id`) ON UPDATE RESTRICT ON DELETE SET NULL,
  CONSTRAINT `ck_creation_sessions_status` CHECK (`status` IN ('direction_discussion', 'direction_ready', 'direction_confirmed', 'configured', 'planning', 'plan_ready', 'materialized', 'failed')),
  CONSTRAINT `ck_creation_sessions_difficulty` CHECK (`difficulty` IS NULL OR `difficulty` IN ('beginner', 'easy', 'normal', 'hard', 'challenge')),
  CONSTRAINT `ck_creation_sessions_duration` CHECK (`duration_minutes` IS NULL OR `duration_minutes` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @has_legacy_global_key = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = 'BM-sql' AND TABLE_NAME = 'experiment_creation_sessions'
    AND INDEX_NAME = 'uq_creation_materialization_key'
);
SET @ddl = IF(@has_legacy_global_key > 0,
  'ALTER TABLE `experiment_creation_sessions` DROP INDEX `uq_creation_materialization_key`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
CALL `bm_add_index_if_missing`(
  'experiment_creation_sessions', 'idx_creation_materialization_key', '(`materialization_key`)'
);

CREATE TABLE IF NOT EXISTS `experiment_creation_messages` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `session_id` CHAR(36) NOT NULL,
  `role` VARCHAR(20) NOT NULL,
  `content` TEXT NOT NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_creation_messages_session_created` (`session_id`, `created_at`),
  CONSTRAINT `fk_creation_messages_session` FOREIGN KEY (`session_id`) REFERENCES `experiment_creation_sessions` (`id`) ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `ck_creation_messages_role` CHECK (`role` IN ('user', 'assistant'))
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
  CONSTRAINT `fk_plan_drafts_session` FOREIGN KEY (`session_id`) REFERENCES `experiment_creation_sessions` (`id`) ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `ck_plan_drafts_status` CHECK (`status` IN ('ready', 'accepted', 'superseded', 'invalid'))
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
  CONSTRAINT `fk_stages_experiment` FOREIGN KEY (`experiment_id`) REFERENCES `experiments` (`id`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CALL `bm_add_column_if_missing`('experiment_nodes', 'stage_id', 'BIGINT UNSIGNED NULL AFTER `experiment_id`');
CALL `bm_add_column_if_missing`('experiment_nodes', 'parent_node_id', 'BIGINT UNSIGNED NULL AFTER `stage_id`');
CALL `bm_add_index_if_missing`('experiment_nodes', 'idx_nodes_stage_experiment', '(`stage_id`, `experiment_id`)');
CALL `bm_add_index_if_missing`('experiment_nodes', 'idx_nodes_parent_experiment', '(`parent_node_id`, `experiment_id`)');
CALL `bm_add_constraint_if_missing`(
  'experiment_nodes', 'fk_nodes_stage',
  'FOREIGN KEY (`stage_id`, `experiment_id`) REFERENCES `experiment_stages` (`id`, `experiment_id`) ON UPDATE RESTRICT ON DELETE RESTRICT'
);
CALL `bm_add_constraint_if_missing`(
  'experiment_nodes', 'fk_nodes_parent',
  'FOREIGN KEY (`parent_node_id`, `experiment_id`) REFERENCES `experiment_nodes` (`id`, `experiment_id`) ON UPDATE RESTRICT ON DELETE CASCADE'
);
DROP PROCEDURE `bm_add_column_if_missing`;
DROP PROCEDURE `bm_drop_check_if_exists`;
DROP PROCEDURE `bm_add_constraint_if_missing`;
DROP PROCEDURE `bm_add_index_if_missing`;
