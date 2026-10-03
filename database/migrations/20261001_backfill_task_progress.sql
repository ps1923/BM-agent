-- Idempotently initialize per-run task progress on compatible BM-sql installations.
-- The canonical task_progress table is defined in database/schema.sql.

USE `BM-sql`;

INSERT INTO `task_progress` (`experiment_id`, `run_id`, `task_id`, `status`, `completed_at`)
SELECT `er`.`experiment_id`, `er`.`id`, `nt`.`id`,
       CASE WHEN `np`.`status` = 'completed' THEN 'completed' ELSE 'pending' END,
       CASE WHEN `np`.`status` = 'completed'
            THEN COALESCE(`np`.`completed_at`, `np`.`updated_at`, CURRENT_TIMESTAMP(6))
            ELSE NULL END
FROM `experiment_runs` AS `er`
JOIN `node_tasks` AS `nt` ON `nt`.`experiment_id` = `er`.`experiment_id`
LEFT JOIN `node_progress` AS `np`
  ON `np`.`run_id` = `er`.`id`
 AND `np`.`experiment_id` = `er`.`experiment_id`
 AND `np`.`node_id` = `nt`.`node_id`
ON DUPLICATE KEY UPDATE
  `status` = IF(VALUES(`status`) = 'completed', 'completed', `task_progress`.`status`),
  `completed_at` = IF(VALUES(`status`) = 'completed',
      COALESCE(`task_progress`.`completed_at`, VALUES(`completed_at`)),
      `task_progress`.`completed_at`);
