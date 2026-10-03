-- Add immutable node-completion evidence without changing existing snapshots.
-- Safe to rerun on a compatible BM-sql schema.
USE `BM-sql`;

SET @add_node_id = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `workspace_snapshots` ADD COLUMN `node_id` BIGINT UNSIGNED NULL AFTER `workspace_id`',
    'SELECT 1')
  FROM `information_schema`.`COLUMNS`
  WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'workspace_snapshots' AND `COLUMN_NAME` = 'node_id'
);
PREPARE add_node_id_stmt FROM @add_node_id;
EXECUTE add_node_id_stmt;
DEALLOCATE PREPARE add_node_id_stmt;

SET @add_node_unique = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `workspace_snapshots` ADD UNIQUE KEY `uq_workspace_snapshots_node_completion` (`workspace_id`, `node_id`, `snapshot_type`)',
    'SELECT 1')
  FROM `information_schema`.`STATISTICS`
  WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'workspace_snapshots'
    AND `INDEX_NAME` = 'uq_workspace_snapshots_node_completion'
);
PREPARE add_node_unique_stmt FROM @add_node_unique;
EXECUTE add_node_unique_stmt;
DEALLOCATE PREPARE add_node_unique_stmt;

SET @add_node_fk = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `workspace_snapshots` ADD CONSTRAINT `fk_workspace_snapshots_node` FOREIGN KEY (`node_id`) REFERENCES `experiment_nodes` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT',
    'SELECT 1')
  FROM `information_schema`.`REFERENTIAL_CONSTRAINTS`
  WHERE `CONSTRAINT_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'workspace_snapshots'
    AND `CONSTRAINT_NAME` = 'fk_workspace_snapshots_node'
);
PREPARE add_node_fk_stmt FROM @add_node_fk;
EXECUTE add_node_fk_stmt;
DEALLOCATE PREPARE add_node_fk_stmt;
