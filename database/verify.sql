-- Read-only verification for the BM-sql schema.
-- Does not USE the target database, so a missing database is reported instead of aborting.

SELECT
  VERSION() AS `server_version`,
  CASE
    WHEN CAST(SUBSTRING_INDEX(VERSION(), '.', 1) AS UNSIGNED) > 8 THEN 'OK'
    WHEN CAST(SUBSTRING_INDEX(VERSION(), '.', 1) AS UNSIGNED) = 8
      AND CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(VERSION(), '.', 2), '.', -1) AS UNSIGNED) > 0 THEN 'OK'
    WHEN CAST(SUBSTRING_INDEX(VERSION(), '.', 1) AS UNSIGNED) = 8
      AND CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(VERSION(), '.', 2), '.', -1) AS UNSIGNED) = 0
      AND CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(VERSION(), '.', 3), '.', -1) AS UNSIGNED) >= 16 THEN 'OK'
    ELSE 'UNSUPPORTED'
  END AS `check_constraint_support`;

SELECT
  `SCHEMA_NAME` AS `database_name`,
  `DEFAULT_CHARACTER_SET_NAME` AS `character_set`,
  `DEFAULT_COLLATION_NAME` AS `collation`,
  CASE
    WHEN `DEFAULT_CHARACTER_SET_NAME` = 'utf8mb4'
      AND `DEFAULT_COLLATION_NAME` = 'utf8mb4_unicode_ci' THEN 'OK'
    ELSE 'MISMATCH'
  END AS `status`
FROM `information_schema`.`SCHEMATA`
WHERE `SCHEMA_NAME` = 'BM-sql';

SELECT
  expected.`table_name`,
  CASE WHEN actual.`TABLE_NAME` IS NULL THEN 'MISSING' ELSE 'OK' END AS `status`
FROM (
  SELECT 'users' AS `table_name`
  UNION ALL SELECT 'user_sessions'
  UNION ALL SELECT 'courses'
  UNION ALL SELECT 'course_members'
  UNION ALL SELECT 'course_experiment_assignments'
  UNION ALL SELECT 'experiment_folders'
  UNION ALL SELECT 'experiments'
  UNION ALL SELECT 'experiment_creation_sessions'
  UNION ALL SELECT 'experiment_creation_messages'
  UNION ALL SELECT 'experiment_plan_drafts'
  UNION ALL SELECT 'experiment_stages'
  UNION ALL SELECT 'experiment_nodes'
  UNION ALL SELECT 'node_dependencies'
  UNION ALL SELECT 'node_tasks'
  UNION ALL SELECT 'experiment_runs'
  UNION ALL SELECT 'node_progress'
  UNION ALL SELECT 'task_progress'
  UNION ALL SELECT 'coding_workspaces'
  UNION ALL SELECT 'workspace_files'
  UNION ALL SELECT 'workspace_snapshots'
  UNION ALL SELECT 'terminal_sessions'
  UNION ALL SELECT 'command_runs'
  UNION ALL SELECT 'chat_messages'
  UNION ALL SELECT 'bug_cases'
  UNION ALL SELECT 'bug_case_reviews'
  UNION ALL SELECT 'diagnosis_tasks'
  UNION ALL SELECT 'code_patches'
  UNION ALL SELECT 'teacher_reviews'
) AS expected
LEFT JOIN `information_schema`.`TABLES` AS actual
  ON actual.`TABLE_SCHEMA` = 'BM-sql'
 AND actual.`TABLE_NAME` = expected.`table_name`
 AND actual.`TABLE_TYPE` = 'BASE TABLE'
ORDER BY expected.`table_name`;

SELECT
  expected.`constraint_name`,
  expected.`table_name`,
  expected.`referenced_table_name`,
  expected.`delete_rule`,
  CASE
    WHEN actual.`CONSTRAINT_NAME` IS NULL THEN 'MISSING'
    WHEN actual.`TABLE_NAME` <> expected.`table_name`
      OR actual.`REFERENCED_TABLE_NAME` <> expected.`referenced_table_name`
      OR actual.`DELETE_RULE` <> expected.`delete_rule` THEN 'MISMATCH'
    ELSE 'OK'
  END AS `status`
FROM (
  SELECT 'fk_folders_owner' AS `constraint_name`, 'experiment_folders' AS `table_name`, 'users' AS `referenced_table_name`, 'RESTRICT' AS `delete_rule`
  UNION ALL SELECT 'fk_user_sessions_user', 'user_sessions', 'users', 'CASCADE'
  UNION ALL SELECT 'fk_courses_teacher', 'courses', 'users', 'RESTRICT'
  UNION ALL SELECT 'fk_course_members_course', 'course_members', 'courses', 'CASCADE'
  UNION ALL SELECT 'fk_course_members_student', 'course_members', 'users', 'RESTRICT'
  UNION ALL SELECT 'fk_assignments_course', 'course_experiment_assignments', 'courses', 'CASCADE'
  UNION ALL SELECT 'fk_assignments_experiment', 'course_experiment_assignments', 'experiments', 'CASCADE'
  UNION ALL SELECT 'fk_assignments_publisher', 'course_experiment_assignments', 'users', 'RESTRICT'
  UNION ALL SELECT 'fk_experiments_creator', 'experiments', 'users', 'RESTRICT'
  UNION ALL SELECT 'fk_experiments_folder_owner', 'experiments', 'experiment_folders', 'RESTRICT'
  UNION ALL SELECT 'fk_creation_sessions_user', 'experiment_creation_sessions', 'users', 'RESTRICT'
  UNION ALL SELECT 'fk_creation_sessions_experiment', 'experiment_creation_sessions', 'experiments', 'SET NULL'
  UNION ALL SELECT 'fk_creation_messages_session', 'experiment_creation_messages', 'experiment_creation_sessions', 'CASCADE'
  UNION ALL SELECT 'fk_plan_drafts_session', 'experiment_plan_drafts', 'experiment_creation_sessions', 'CASCADE'
  UNION ALL SELECT 'fk_stages_experiment', 'experiment_stages', 'experiments', 'CASCADE'
  UNION ALL SELECT 'fk_nodes_experiment', 'experiment_nodes', 'experiments', 'CASCADE'
  UNION ALL SELECT 'fk_nodes_stage', 'experiment_nodes', 'experiment_stages', 'RESTRICT'
  UNION ALL SELECT 'fk_nodes_parent', 'experiment_nodes', 'experiment_nodes', 'CASCADE'
  UNION ALL SELECT 'fk_dependencies_from_node', 'node_dependencies', 'experiment_nodes', 'CASCADE'
  UNION ALL SELECT 'fk_dependencies_to_node', 'node_dependencies', 'experiment_nodes', 'CASCADE'
  UNION ALL SELECT 'fk_tasks_node', 'node_tasks', 'experiment_nodes', 'CASCADE'
  UNION ALL SELECT 'fk_runs_experiment', 'experiment_runs', 'experiments', 'RESTRICT'
  UNION ALL SELECT 'fk_runs_student', 'experiment_runs', 'users', 'RESTRICT'
  UNION ALL SELECT 'fk_runs_current_node', 'experiment_runs', 'experiment_nodes', 'RESTRICT'
  UNION ALL SELECT 'fk_node_progress_run', 'node_progress', 'experiment_runs', 'CASCADE'
  UNION ALL SELECT 'fk_node_progress_node', 'node_progress', 'experiment_nodes', 'RESTRICT'
  UNION ALL SELECT 'fk_task_progress_run', 'task_progress', 'experiment_runs', 'CASCADE'
  UNION ALL SELECT 'fk_task_progress_task', 'task_progress', 'node_tasks', 'RESTRICT'
  UNION ALL SELECT 'fk_coding_workspaces_run', 'coding_workspaces', 'experiment_runs', 'CASCADE'
  UNION ALL SELECT 'fk_workspace_files_workspace', 'workspace_files', 'coding_workspaces', 'CASCADE'
  UNION ALL SELECT 'fk_workspace_snapshots_workspace', 'workspace_snapshots', 'coding_workspaces', 'CASCADE'
  UNION ALL SELECT 'fk_workspace_snapshots_node', 'workspace_snapshots', 'experiment_nodes', 'RESTRICT'
  UNION ALL SELECT 'fk_terminal_sessions_workspace', 'terminal_sessions', 'coding_workspaces', 'CASCADE'
  UNION ALL SELECT 'fk_command_runs_terminal', 'command_runs', 'terminal_sessions', 'CASCADE'
  UNION ALL SELECT 'fk_chat_messages_run', 'chat_messages', 'experiment_runs', 'CASCADE'
  UNION ALL SELECT 'fk_chat_messages_node', 'chat_messages', 'experiment_nodes', 'RESTRICT'
  UNION ALL SELECT 'fk_bug_cases_run', 'bug_cases', 'experiment_runs', 'CASCADE'
  UNION ALL SELECT 'fk_bug_cases_node', 'bug_cases', 'experiment_nodes', 'RESTRICT'
  UNION ALL SELECT 'fk_bug_cases_course', 'bug_cases', 'courses', 'SET NULL'
  UNION ALL SELECT 'fk_bug_cases_student', 'bug_cases', 'users', 'RESTRICT'
  UNION ALL SELECT 'fk_bug_case_reviews_case', 'bug_case_reviews', 'bug_cases', 'CASCADE'
  UNION ALL SELECT 'fk_bug_case_reviews_teacher', 'bug_case_reviews', 'users', 'RESTRICT'
  UNION ALL SELECT 'fk_diagnosis_tasks_run', 'diagnosis_tasks', 'experiment_runs', 'CASCADE'
  UNION ALL SELECT 'fk_diagnosis_tasks_workspace', 'diagnosis_tasks', 'coding_workspaces', 'CASCADE'
  UNION ALL SELECT 'fk_diagnosis_tasks_student', 'diagnosis_tasks', 'users', 'RESTRICT'
  UNION ALL SELECT 'fk_code_patches_run', 'code_patches', 'experiment_runs', 'CASCADE'
  UNION ALL SELECT 'fk_code_patches_workspace', 'code_patches', 'coding_workspaces', 'CASCADE'
  UNION ALL SELECT 'fk_code_patches_student', 'code_patches', 'users', 'RESTRICT'
  UNION ALL SELECT 'fk_code_patches_snapshot', 'code_patches', 'workspace_snapshots', 'SET NULL'
  UNION ALL SELECT 'fk_teacher_reviews_course', 'teacher_reviews', 'courses', 'CASCADE'
  UNION ALL SELECT 'fk_teacher_reviews_run', 'teacher_reviews', 'experiment_runs', 'CASCADE'
  UNION ALL SELECT 'fk_teacher_reviews_teacher', 'teacher_reviews', 'users', 'RESTRICT'
  UNION ALL SELECT 'fk_teacher_reviews_student', 'teacher_reviews', 'users', 'RESTRICT'
) AS expected
LEFT JOIN `information_schema`.`REFERENTIAL_CONSTRAINTS` AS actual
  ON actual.`CONSTRAINT_SCHEMA` = 'BM-sql'
 AND actual.`CONSTRAINT_NAME` = expected.`constraint_name`
ORDER BY expected.`table_name`, expected.`constraint_name`;

SELECT
  `TABLE_NAME` AS `non_utf8mb4_table`,
  `TABLE_COLLATION` AS `actual_collation`
FROM `information_schema`.`TABLES`
WHERE `TABLE_SCHEMA` = 'BM-sql'
  AND `TABLE_TYPE` = 'BASE TABLE'
  AND (`TABLE_COLLATION` IS NULL OR `TABLE_COLLATION` <> 'utf8mb4_unicode_ci');

SELECT
  `COLUMN_NAME` AS `unexpected_source_content_column`,
  'MISMATCH' AS `status`
FROM `information_schema`.`COLUMNS`
WHERE `TABLE_SCHEMA` = 'BM-sql'
  AND `TABLE_NAME` = 'workspace_files'
  AND `COLUMN_NAME` IN ('content', 'source_code', 'file_content');

-- Creation-flow columns that are intentionally excluded from the legacy full-table
-- fingerprints below. This keeps the verification readable while still catching
-- missing or incorrectly typed migration fields.
SELECT
  expected.`table_name`,
  expected.`column_name`,
  expected.`column_type`,
  expected.`is_nullable`,
  CASE
    WHEN actual.`COLUMN_NAME` IS NULL THEN 'MISSING'
    WHEN actual.`COLUMN_TYPE` <> expected.`column_type`
      OR actual.`IS_NULLABLE` <> expected.`is_nullable` THEN 'MISMATCH'
    ELSE 'OK'
  END AS `status`
FROM (
  SELECT 'users' AS `table_name`, 'password_hash' AS `column_name`, 'varchar(255)' AS `column_type`, 'YES' AS `is_nullable`
  UNION ALL SELECT 'users', 'role', 'varchar(20)', 'NO'
  UNION ALL SELECT 'users', 'status', 'varchar(20)', 'NO'
  UNION ALL SELECT 'courses', 'invite_code', 'varchar(16)', 'NO'
  UNION ALL SELECT 'course_members', 'status', 'varchar(20)', 'NO'
  UNION ALL SELECT 'bug_cases', 'technology_stack', 'varchar(100)', 'YES'
  UNION ALL SELECT 'course_experiment_assignments', 'published_at', 'datetime(6)', 'NO'
  UNION ALL SELECT 'experiments' AS `table_name`, 'duration_minutes' AS `column_name`, 'int unsigned' AS `column_type`, 'NO' AS `is_nullable`
  UNION ALL SELECT 'experiment_nodes', 'stage_id', 'bigint unsigned', 'YES'
  UNION ALL SELECT 'experiment_nodes', 'parent_node_id', 'bigint unsigned', 'YES'
  UNION ALL SELECT 'experiment_creation_sessions', 'direction_envelope', 'json', 'YES'
  UNION ALL SELECT 'experiment_creation_sessions', 'confirmed_intent', 'json', 'YES'
  UNION ALL SELECT 'experiment_creation_sessions', 'intent_hash', 'char(64)', 'YES'
  UNION ALL SELECT 'experiment_creation_sessions', 'duration_minutes', 'int unsigned', 'YES'
  UNION ALL SELECT 'user_sessions', 'token_hash', 'char(64)', 'NO'
  UNION ALL SELECT 'user_sessions', 'expires_at', 'datetime(6)', 'NO'
  UNION ALL SELECT 'experiment_plan_drafts', 'plan_json', 'json', 'NO'
  UNION ALL SELECT 'experiment_stages', 'sort_order', 'int', 'NO'
) AS expected
LEFT JOIN `information_schema`.`COLUMNS` AS actual
  ON actual.`TABLE_SCHEMA` = 'BM-sql'
 AND actual.`TABLE_NAME` = expected.`table_name`
 AND actual.`COLUMN_NAME` = expected.`column_name`
ORDER BY expected.`table_name`, expected.`column_name`;

SELECT
  expected.`table_name`,
  expected.`index_name`,
  CASE
    WHEN actual.`INDEX_NAME` IS NULL THEN 'MISSING'
    WHEN actual.`NON_UNIQUE` <> expected.`non_unique` THEN 'MISMATCH'
    ELSE 'OK'
  END AS `status`
FROM (
  SELECT 'experiment_creation_sessions' AS `table_name`, 'PRIMARY' AS `index_name`, 0 AS `non_unique`
  UNION ALL SELECT 'experiment_creation_sessions', 'idx_creation_materialization_key', 1
  UNION ALL SELECT 'experiment_creation_messages', 'idx_creation_messages_session_created', 1
  UNION ALL SELECT 'experiment_plan_drafts', 'uq_plan_drafts_session_version', 0
  UNION ALL SELECT 'experiment_stages', 'uq_stages_experiment_order', 0
  UNION ALL SELECT 'experiment_nodes', 'idx_nodes_stage_experiment', 1
  UNION ALL SELECT 'experiment_nodes', 'idx_nodes_parent_experiment', 1
  UNION ALL SELECT 'user_sessions', 'PRIMARY', 0
  UNION ALL SELECT 'user_sessions', 'uq_user_sessions_token_hash', 0
  UNION ALL SELECT 'user_sessions', 'idx_user_sessions_user_active', 1
  UNION ALL SELECT 'courses', 'PRIMARY', 0
  UNION ALL SELECT 'courses', 'uq_courses_invite_code', 0
  UNION ALL SELECT 'course_members', 'PRIMARY', 0
  UNION ALL SELECT 'course_experiment_assignments', 'PRIMARY', 0
  UNION ALL SELECT 'course_experiment_assignments', 'uq_course_experiment_assignment', 0
) AS expected
LEFT JOIN (
  SELECT `TABLE_NAME`, `INDEX_NAME`, MIN(`NON_UNIQUE`) AS `NON_UNIQUE`
  FROM `information_schema`.`STATISTICS`
  WHERE `TABLE_SCHEMA` = 'BM-sql'
  GROUP BY `TABLE_NAME`, `INDEX_NAME`
) AS actual
  ON actual.`TABLE_NAME` = expected.`table_name`
 AND actual.`INDEX_NAME` = expected.`index_name`
ORDER BY expected.`table_name`, expected.`index_name`;

SELECT CASE WHEN `column_list` = 'invite_code' THEN 'OK' ELSE 'MISMATCH' END AS `course_invite_index_columns_status`,
       `column_list`
FROM (
  SELECT COALESCE(GROUP_CONCAT(`COLUMN_NAME` ORDER BY `SEQ_IN_INDEX` SEPARATOR ','), '') AS `column_list`
  FROM `information_schema`.`STATISTICS`
  WHERE `TABLE_SCHEMA` = 'BM-sql' AND `TABLE_NAME` = 'courses'
    AND `INDEX_NAME` = 'uq_courses_invite_code' AND `NON_UNIQUE` = 0
) AS actual;

SELECT CASE WHEN `column_list` = 'token_hash' THEN 'OK' ELSE 'MISMATCH' END AS `session_token_index_columns_status`,
       `column_list`
FROM (
  SELECT COALESCE(GROUP_CONCAT(`COLUMN_NAME` ORDER BY `SEQ_IN_INDEX` SEPARATOR ','), '') AS `column_list`
  FROM `information_schema`.`STATISTICS`
  WHERE `TABLE_SCHEMA` = 'BM-sql' AND `TABLE_NAME` = 'user_sessions'
    AND `INDEX_NAME` = 'uq_user_sessions_token_hash' AND `NON_UNIQUE` = 0
) AS actual;

SELECT expected.`index_name`, COALESCE(actual.`column_list`, '') AS `column_list`,
       CASE WHEN actual.`column_list` = expected.`expected_columns` THEN 'OK' ELSE 'MISMATCH' END AS `column_status`
FROM (
  SELECT 'uq_course_experiment_assignment' AS `index_name`, 'course_id,experiment_id' AS `expected_columns`
) AS expected
LEFT JOIN (
  SELECT `INDEX_NAME`, GROUP_CONCAT(`COLUMN_NAME` ORDER BY `SEQ_IN_INDEX` SEPARATOR ',') AS `column_list`
  FROM `information_schema`.`STATISTICS`
  WHERE `TABLE_SCHEMA` = 'BM-sql' AND `TABLE_NAME` = 'course_experiment_assignments'
    AND `NON_UNIQUE` = 0 AND `INDEX_NAME` = 'uq_course_experiment_assignment'
  GROUP BY `INDEX_NAME`
) AS actual ON actual.`INDEX_NAME` = expected.`index_name`;

SELECT expected.`constraint_name`,
       CASE WHEN actual.`constraint_name` IS NULL THEN 'MISSING'
            WHEN actual.`column_list` <> expected.`column_list`
              OR actual.`referenced_table_name` <> expected.`referenced_table_name`
              OR actual.`referenced_column_list` <> expected.`referenced_column_list`
              OR actual.`DELETE_RULE` <> expected.`delete_rule`
              OR actual.`UPDATE_RULE` <> 'RESTRICT' THEN 'MISMATCH'
            ELSE 'OK' END AS `definition_status`
FROM (
  SELECT 'fk_courses_teacher' AS `constraint_name`, 'courses' AS `table_name`, 'teacher_id' AS `column_list`,
         'users' AS `referenced_table_name`, 'id' AS `referenced_column_list`, 'RESTRICT' AS `delete_rule`
  UNION ALL SELECT 'fk_user_sessions_user', 'user_sessions', 'user_id', 'users', 'id', 'CASCADE'
  UNION ALL SELECT 'fk_course_members_course', 'course_members', 'course_id', 'courses', 'id', 'CASCADE'
  UNION ALL SELECT 'fk_course_members_student', 'course_members', 'student_id', 'users', 'id', 'RESTRICT'
  UNION ALL SELECT 'fk_assignments_course', 'course_experiment_assignments', 'course_id', 'courses', 'id', 'CASCADE'
  UNION ALL SELECT 'fk_assignments_experiment', 'course_experiment_assignments', 'experiment_id', 'experiments', 'id', 'CASCADE'
  UNION ALL SELECT 'fk_assignments_publisher', 'course_experiment_assignments', 'publisher_id', 'users', 'id', 'RESTRICT'
  UNION ALL SELECT 'fk_bug_cases_run', 'bug_cases', 'run_id,experiment_id', 'experiment_runs', 'id,experiment_id', 'CASCADE'
  UNION ALL SELECT 'fk_bug_cases_node', 'bug_cases', 'node_id,experiment_id', 'experiment_nodes', 'id,experiment_id', 'RESTRICT'
  UNION ALL SELECT 'fk_bug_cases_course', 'bug_cases', 'course_id', 'courses', 'id', 'SET NULL'
  UNION ALL SELECT 'fk_bug_cases_student', 'bug_cases', 'student_id', 'users', 'id', 'RESTRICT'
  UNION ALL SELECT 'fk_bug_case_reviews_case', 'bug_case_reviews', 'bug_case_id', 'bug_cases', 'id', 'CASCADE'
  UNION ALL SELECT 'fk_bug_case_reviews_teacher', 'bug_case_reviews', 'teacher_id', 'users', 'id', 'RESTRICT'
  UNION ALL SELECT 'fk_diagnosis_tasks_run', 'diagnosis_tasks', 'run_id,experiment_id', 'experiment_runs', 'id,experiment_id', 'CASCADE'
  UNION ALL SELECT 'fk_diagnosis_tasks_workspace', 'diagnosis_tasks', 'workspace_id', 'coding_workspaces', 'id', 'CASCADE'
  UNION ALL SELECT 'fk_diagnosis_tasks_student', 'diagnosis_tasks', 'student_id', 'users', 'id', 'RESTRICT'
  UNION ALL SELECT 'fk_code_patches_run', 'code_patches', 'run_id,experiment_id', 'experiment_runs', 'id,experiment_id', 'CASCADE'
  UNION ALL SELECT 'fk_code_patches_workspace', 'code_patches', 'workspace_id', 'coding_workspaces', 'id', 'CASCADE'
  UNION ALL SELECT 'fk_code_patches_student', 'code_patches', 'student_id', 'users', 'id', 'RESTRICT'
  UNION ALL SELECT 'fk_code_patches_snapshot', 'code_patches', 'applied_snapshot_id', 'workspace_snapshots', 'id', 'SET NULL'
  UNION ALL SELECT 'fk_teacher_reviews_course', 'teacher_reviews', 'course_id', 'courses', 'id', 'CASCADE'
  UNION ALL SELECT 'fk_teacher_reviews_run', 'teacher_reviews', 'run_id,experiment_id', 'experiment_runs', 'id,experiment_id', 'CASCADE'
  UNION ALL SELECT 'fk_teacher_reviews_teacher', 'teacher_reviews', 'teacher_id', 'users', 'id', 'RESTRICT'
  UNION ALL SELECT 'fk_teacher_reviews_student', 'teacher_reviews', 'student_id', 'users', 'id', 'RESTRICT'
  UNION ALL SELECT 'fk_workspace_snapshots_node', 'workspace_snapshots', 'node_id', 'experiment_nodes', 'id', 'RESTRICT'
) AS expected
LEFT JOIN (
  SELECT rc.`CONSTRAINT_NAME` AS `constraint_name`, rc.`TABLE_NAME`,
         GROUP_CONCAT(kcu.`COLUMN_NAME` ORDER BY kcu.`ORDINAL_POSITION` SEPARATOR ',') AS `column_list`,
         MAX(kcu.`REFERENCED_TABLE_NAME`) AS `referenced_table_name`,
         GROUP_CONCAT(kcu.`REFERENCED_COLUMN_NAME` ORDER BY kcu.`ORDINAL_POSITION` SEPARATOR ',') AS `referenced_column_list`,
         rc.`DELETE_RULE`, rc.`UPDATE_RULE`
  FROM `information_schema`.`REFERENTIAL_CONSTRAINTS` AS rc
  JOIN `information_schema`.`KEY_COLUMN_USAGE` AS kcu
    ON kcu.`CONSTRAINT_SCHEMA` = rc.`CONSTRAINT_SCHEMA`
   AND kcu.`TABLE_NAME` = rc.`TABLE_NAME` AND kcu.`CONSTRAINT_NAME` = rc.`CONSTRAINT_NAME`
  WHERE rc.`CONSTRAINT_SCHEMA` = 'BM-sql'
  GROUP BY rc.`CONSTRAINT_NAME`, rc.`TABLE_NAME`, rc.`DELETE_RULE`, rc.`UPDATE_RULE`
) AS actual
  ON actual.`TABLE_NAME` = expected.`table_name` AND actual.`constraint_name` = expected.`constraint_name`
ORDER BY expected.`constraint_name`;

SELECT
  expected.`table_name`, expected.`constraint_name`,
  CASE WHEN actual.`CONSTRAINT_NAME` IS NULL THEN 'MISSING' ELSE 'OK' END AS `status`
FROM (
  SELECT 'users' AS `table_name`, 'ck_users_role' AS `constraint_name`
  UNION ALL SELECT 'users', 'ck_users_status'
  UNION ALL SELECT 'courses', 'ck_courses_status'
  UNION ALL SELECT 'course_members', 'ck_course_members_status'
  UNION ALL SELECT 'course_experiment_assignments', 'ck_assignments_status'
  UNION ALL SELECT 'experiments' AS `table_name`, 'ck_experiments_duration_minutes' AS `constraint_name`
  UNION ALL SELECT 'experiment_creation_sessions', 'ck_creation_sessions_status'
  UNION ALL SELECT 'experiment_creation_sessions', 'ck_creation_sessions_difficulty'
  UNION ALL SELECT 'experiment_creation_sessions', 'ck_creation_sessions_duration'
  UNION ALL SELECT 'experiment_creation_messages', 'ck_creation_messages_role'
  UNION ALL SELECT 'experiment_plan_drafts', 'ck_plan_drafts_status'
  UNION ALL SELECT 'coding_workspaces', 'ck_coding_workspaces_status'
  UNION ALL SELECT 'coding_workspaces', 'ck_coding_workspaces_cpu'
  UNION ALL SELECT 'coding_workspaces', 'ck_coding_workspaces_memory'
  UNION ALL SELECT 'coding_workspaces', 'ck_coding_workspaces_expired_at'
  UNION ALL SELECT 'bug_cases', 'ck_bug_cases_status'
  UNION ALL SELECT 'bug_cases', 'ck_bug_cases_visibility'
  UNION ALL SELECT 'bug_cases', 'ck_bug_cases_vector_status'
  UNION ALL SELECT 'bug_case_reviews', 'ck_bug_case_reviews_decision'
  UNION ALL SELECT 'diagnosis_tasks', 'ck_diagnosis_tasks_status'
  UNION ALL SELECT 'code_patches', 'ck_code_patches_status'
  UNION ALL SELECT 'code_patches', 'ck_code_patches_applied_at'
  UNION ALL SELECT 'teacher_reviews', 'ck_teacher_reviews_rating'
) AS expected
LEFT JOIN `information_schema`.`TABLE_CONSTRAINTS` AS actual
  ON actual.`CONSTRAINT_SCHEMA` = 'BM-sql'
 AND actual.`TABLE_NAME` = expected.`table_name`
 AND actual.`CONSTRAINT_NAME` = expected.`constraint_name`
 AND actual.`CONSTRAINT_TYPE` = 'CHECK'
ORDER BY expected.`table_name`, expected.`constraint_name`;

SELECT expected.`table_name`, expected.`constraint_name`,
       CASE WHEN actual.`CHECK_CLAUSE` IS NULL THEN 'MISSING'
            WHEN LOWER(REPLACE(REPLACE(REPLACE(REPLACE(actual.`CHECK_CLAUSE`, '`', ''), ' ', ''), '(', ''), ')', ''))
                 = expected.`normalized_clause` THEN 'OK'
            ELSE 'MISMATCH' END AS `definition_status`
FROM (
  SELECT 'coding_workspaces' AS `table_name`, 'ck_coding_workspaces_status' AS `constraint_name`,
    'statusin(''provisioning'',''starting'',''running'',''stopped'',''error'',''expired'')' AS `normalized_clause`
  UNION ALL SELECT 'courses', 'ck_courses_status', 'statusin(''active'',''archived'')'
  UNION ALL SELECT 'course_members', 'ck_course_members_status', 'statusin(''active'',''left'')'
  UNION ALL SELECT 'course_experiment_assignments', 'ck_assignments_status', 'statusin(''published'',''archived'')'
  UNION ALL SELECT 'coding_workspaces', 'ck_coding_workspaces_cpu', 'cpu_limitisnullorcpu_limit>0'
  UNION ALL SELECT 'coding_workspaces', 'ck_coding_workspaces_memory', 'memory_limit_mbisnullormemory_limit_mb>0'
  UNION ALL SELECT 'coding_workspaces', 'ck_coding_workspaces_expired_at', 'status<>''expired''orexpires_atisnotnull'
  UNION ALL SELECT 'bug_cases', 'ck_bug_cases_status', 'statusin(''pending'',''approved'',''rejected'')'
  UNION ALL SELECT 'bug_cases', 'ck_bug_cases_visibility', 'visibilityin(''personal'',''course'')'
  UNION ALL SELECT 'bug_cases', 'ck_bug_cases_vector_status', 'vector_statusin(''not_indexed'',''indexed'',''failed'')'
  UNION ALL SELECT 'bug_case_reviews', 'ck_bug_case_reviews_decision', 'decisionin(''approved'',''rejected'')'
  UNION ALL SELECT 'diagnosis_tasks', 'ck_diagnosis_tasks_status', 'statusin(''queued'',''running'',''completed'',''failed'',''cancelled'')'
  UNION ALL SELECT 'code_patches', 'ck_code_patches_status', 'statusin(''pending'',''applied'',''rejected'')'
  UNION ALL SELECT 'code_patches', 'ck_code_patches_applied_at', 'status<>''applied''orapplied_atisnotnull'
  UNION ALL SELECT 'teacher_reviews', 'ck_teacher_reviews_rating', 'ratingisnullorrating>=0andrating<=100'
) AS expected
LEFT JOIN `information_schema`.`CHECK_CONSTRAINTS` AS actual
  ON actual.`CONSTRAINT_SCHEMA` = 'BM-sql'
 AND actual.`CONSTRAINT_NAME` = expected.`constraint_name`
ORDER BY expected.`table_name`, expected.`constraint_name`;

SELECT tc.`TABLE_NAME`, tc.`CONSTRAINT_NAME`, tc.`ENFORCED`,
       'NOT_ENFORCED' AS `enforcement_status`
FROM `information_schema`.`TABLE_CONSTRAINTS` AS tc
WHERE tc.`CONSTRAINT_SCHEMA` = 'BM-sql' AND tc.`CONSTRAINT_TYPE` = 'CHECK'
  AND tc.`ENFORCED` <> 'YES'
ORDER BY tc.`TABLE_NAME`, tc.`CONSTRAINT_NAME`;

SET SESSION group_concat_max_len = 1000000;

WITH expected AS (
  SELECT 'COL' AS `kind`, 'chat_messages' AS `table_name`, '06631f32c5ed5afa7f161a22a2d00b5e580ccd0e6be7b8fd7e9b72d1d08c5e92' AS `fingerprint`
  UNION ALL SELECT 'COL', 'coding_workspaces', '483800d0af4243144161042f0aa149ce32b77013ebab7ffc984ca18a37f4c606'
  UNION ALL SELECT 'COL', 'command_runs', 'afb83058e8f9ff2e1fe37f01fde3f4a3827d483f13c20b96a95bedc36622dc99'
  UNION ALL SELECT 'COL', 'experiment_folders', 'ba63d7f638149d7c76b473a7ed73f214221d94fb55a61baaf1331f226b140b90'
  UNION ALL SELECT 'COL', 'experiment_runs', '62937e9deb06f8e02bec3ecc4b54adcc982dc16b9f73e8b9e0858beb40989235'
  UNION ALL SELECT 'COL', 'node_dependencies', '17cf701aa3d0f998ef6b461daef379cd5ac251569e3c6bcdda50486908dad1e7'
  UNION ALL SELECT 'COL', 'node_progress', 'b6e90e33b8a6ef042978120079d90411a05577479c42e202d29bb8f39dfa987a'
  UNION ALL SELECT 'COL', 'node_tasks', '2fb9da3800d6343faeee5176a261768fa10da886a9f0723f517b02b5172fb9be'
  UNION ALL SELECT 'COL', 'task_progress', '0e9f47c946b6aac2fba240e4250549b3c0f59fecd05b276cbc2095d3cb95ca62'
  UNION ALL SELECT 'COL', 'terminal_sessions', '51284ece365ad7fdeb3a6e9ab45c77397dde5795709b0cd654946308835342d9'
  UNION ALL SELECT 'COL', 'workspace_files', 'a42add6cb94cba1f0b7e3545be10d3a47834a21ea3f56b108c3abe4533accdbf'
  UNION ALL SELECT 'IDX', 'chat_messages', '651f19426b0239b6590d7ae43ef3eb1d242fc693f51960aa2928f55661e1ad6a'
  UNION ALL SELECT 'IDX', 'coding_workspaces', '9d0a538f92d73777ad456acdeb253032c92a66feccece4088bf13e5adc2abdfd'
  UNION ALL SELECT 'IDX', 'command_runs', '04c5bf12c72a5c979e39a9fd6963bc15ccf38956d7932413f5a504e6b3fbbfbc'
  UNION ALL SELECT 'IDX', 'experiment_folders', '56ee339af1e81120113e87129d0b031c91a8141aa37df05700998fd012ca88ed'
  UNION ALL SELECT 'IDX', 'experiment_runs', '75d0ffd9fd725cd1393b1a6aa629e366bc0bd8ee0ec70f141dc8d0a23e165a5f'
  UNION ALL SELECT 'IDX', 'node_dependencies', 'f1af420354c2a295dc484eb9c9c3e6a7991f1004ca717cb2f3932dc1ced5eed8'
  UNION ALL SELECT 'IDX', 'node_progress', '9165bb13fa63fda640e9fcc256629dc96cb13bb540f08d27cf7f52aba0a7f54d'
  UNION ALL SELECT 'IDX', 'node_tasks', '1daf5c8de2054b7184b5905144a0629c871aedf5c7105aa0284f8e894b58ecc6'
  UNION ALL SELECT 'IDX', 'task_progress', '27229ac5a8497b0c6a62921314149dc81caa659e4cddbacb503f664c2ffc48de'
  UNION ALL SELECT 'IDX', 'terminal_sessions', 'c4c5599d3782e1e36ce11c93ac2d648c02c7bc3d17ea26ac46e8df57d8921395'
  UNION ALL SELECT 'IDX', 'workspace_files', '61fb9100dd1f085a23a327d6b51c29153e71e9fbe54b448fdc7bfc9d7f7d569c'
  UNION ALL SELECT 'CHK', 'chat_messages', 'bcc75c37b330f569d31186d8c5045cd743d346b0841aab5d47177a35e52e35ca'
  UNION ALL SELECT 'CHK', 'command_runs', '04f6cba1d114fddc07cc5d92b2b1045c56bca66cff16ad0ce39ad06c9b3b7a14'
  UNION ALL SELECT 'CHK', 'experiment_runs', 'a3188e9655ff4606593a960f27379b77127e9067c5aa289e7c228276339eace9'
  UNION ALL SELECT 'CHK', 'node_dependencies', '6a6a49e182c36b8f08beab0ed691ff24980fcf189e2ea3b827c5bc5b8fb61a2f'
  UNION ALL SELECT 'CHK', 'node_progress', 'db237723803eae5bfba4e7daaefacc44f5e3f3ca7aa19abe900d05ac19ab1fc0'
  UNION ALL SELECT 'CHK', 'node_tasks', '74f788134fa295b8145e7a12184fad63d560d63a7014d9c886dfdf1055cf667d'
  UNION ALL SELECT 'CHK', 'task_progress', '3976156ff51cfe4906a20924b9d5e0d847cc57fa0421062566d9d7671c958f07'
  UNION ALL SELECT 'CHK', 'terminal_sessions', 'eb3b625561bdc12fb0c0258074f2676261b826b8d8949c61946b796176fc699b'
  UNION ALL SELECT 'CHK', 'workspace_files', 'c2f08e920cc2efa2ce13cbd12aa855e24d55bb3e472cf371cd0b008df0226401'
  UNION ALL SELECT 'CHK', 'workspace_snapshots', '6570c70f549e436a1abd17f7802b21c83eae563d3aa150ff443b892b9b250e02'
  UNION ALL SELECT 'FK', 'chat_messages', 'b4ba51340e43f21e98770a2beb86bcfa43097cca5e6acf873593cade8c208d3d'
  UNION ALL SELECT 'FK', 'coding_workspaces', '3f805d5961fd69640bd038796ff1b7bed04a3722d59d70ef0c31d0af1b9a507c'
  UNION ALL SELECT 'FK', 'command_runs', 'e07befd9b430ed9efa9db7bf1ce344ea4265524aa1e4f474958f3e04ffc493c5'
  UNION ALL SELECT 'FK', 'experiment_folders', '65698c14f3010454809dd3ec8b9d61775033b5c514a8d74e2cc41ce3dee1f2c1'
  UNION ALL SELECT 'FK', 'experiment_runs', '83100d008ea0beabccdef3ec6b1ede1058ab20c99eda5daaaab77c5eac542aba'
  UNION ALL SELECT 'FK', 'node_dependencies', '0a642e6b6d2edbce4267af496b0060cb60cdcd97f909e172d4f006fbc7ce483b'
  UNION ALL SELECT 'FK', 'node_progress', 'f82b417ea49abbc3b7668bd09a4c29db8d3e3c7535937bc69e349340e6838385'
  UNION ALL SELECT 'FK', 'node_tasks', 'd70c84ac2821cd36c781d31d3a121f99c729d6c5b076b42281d9b8ba4866dac6'
  UNION ALL SELECT 'FK', 'task_progress', '4cc86fad8df2e253f71aa9d086656549e570ed989020de47fb3687e45746a562'
  UNION ALL SELECT 'FK', 'terminal_sessions', 'b2cf9291f60984133b52876e55fdbcb915d9529d2317c5ed695d8a8b62cfa886'
  UNION ALL SELECT 'FK', 'workspace_files', '8f0da70fa0a99c94188796fe232337a6f66451d292fa8f9e40aaa3b11ef08e0c'
), actual AS (
  SELECT 'COL' AS `kind`, `TABLE_NAME` AS `table_name`,
    SHA2(GROUP_CONCAT(CONCAT_WS('#', `COLUMN_NAME`, `COLUMN_TYPE`, `IS_NULLABLE`,
      COALESCE(`COLUMN_DEFAULT`, '<NULL>'), `EXTRA`, COALESCE(`COLLATION_NAME`, ''))
      ORDER BY `ORDINAL_POSITION` SEPARATOR '|'), 256) AS `fingerprint`
  FROM `information_schema`.`COLUMNS`
  WHERE `TABLE_SCHEMA` = 'BM-sql'
  GROUP BY `TABLE_NAME`
  UNION ALL
  SELECT 'IDX', `TABLE_NAME`,
    SHA2(GROUP_CONCAT(CONCAT_WS('#', `INDEX_NAME`, `NON_UNIQUE`, `SEQ_IN_INDEX`, `COLUMN_NAME`,
      COALESCE(`SUB_PART`, '<NULL>'), COALESCE(`COLLATION`, ''))
      ORDER BY `INDEX_NAME`, `SEQ_IN_INDEX` SEPARATOR '|'), 256)
  FROM `information_schema`.`STATISTICS`
  WHERE `TABLE_SCHEMA` = 'BM-sql'
  GROUP BY `TABLE_NAME`
  UNION ALL
  SELECT 'CHK', tc.`TABLE_NAME`,
    SHA2(GROUP_CONCAT(CONCAT_WS('#', tc.`CONSTRAINT_NAME`, cc.`CHECK_CLAUSE`)
      ORDER BY tc.`CONSTRAINT_NAME` SEPARATOR '|'), 256)
  FROM `information_schema`.`TABLE_CONSTRAINTS` AS tc
  JOIN `information_schema`.`CHECK_CONSTRAINTS` AS cc
    ON cc.`CONSTRAINT_SCHEMA` = tc.`CONSTRAINT_SCHEMA`
   AND cc.`CONSTRAINT_NAME` = tc.`CONSTRAINT_NAME`
  WHERE tc.`CONSTRAINT_SCHEMA` = 'BM-sql'
    AND tc.`CONSTRAINT_TYPE` = 'CHECK'
  GROUP BY tc.`TABLE_NAME`
  UNION ALL
  SELECT 'FK', rc.`TABLE_NAME`,
    SHA2(GROUP_CONCAT(CONCAT_WS('#', rc.`CONSTRAINT_NAME`, rc.`UPDATE_RULE`, rc.`DELETE_RULE`,
      kcu.`ORDINAL_POSITION`, kcu.`COLUMN_NAME`, kcu.`REFERENCED_TABLE_NAME`,
      kcu.`REFERENCED_COLUMN_NAME`) ORDER BY rc.`CONSTRAINT_NAME`, kcu.`ORDINAL_POSITION` SEPARATOR '|'), 256)
  FROM `information_schema`.`REFERENTIAL_CONSTRAINTS` AS rc
  JOIN `information_schema`.`KEY_COLUMN_USAGE` AS kcu
    ON kcu.`CONSTRAINT_SCHEMA` = rc.`CONSTRAINT_SCHEMA`
   AND kcu.`CONSTRAINT_NAME` = rc.`CONSTRAINT_NAME`
   AND kcu.`TABLE_NAME` = rc.`TABLE_NAME`
  WHERE rc.`CONSTRAINT_SCHEMA` = 'BM-sql'
  GROUP BY rc.`TABLE_NAME`
)
SELECT
  expected.`kind`,
  expected.`table_name`,
  CASE
    WHEN actual.`fingerprint` IS NULL THEN 'MISSING'
    WHEN actual.`fingerprint` <> expected.`fingerprint` THEN 'MISMATCH'
    ELSE 'OK'
  END AS `status`
FROM expected
LEFT JOIN actual
  ON actual.`kind` = expected.`kind`
 AND actual.`table_name` = expected.`table_name`
ORDER BY expected.`kind`, expected.`table_name`;

SELECT
  CASE WHEN c.`COLUMN_NAME` IS NOT NULL AND s.`INDEX_NAME` IS NOT NULL
            AND f.`CONSTRAINT_NAME` IS NOT NULL THEN 'OK' ELSE 'MISMATCH' END AS `node_snapshot_structure_status`,
  c.`COLUMN_NAME` AS `node_reference_column`,
  s.`INDEX_NAME` AS `idempotency_index`,
  f.`CONSTRAINT_NAME` AS `node_foreign_key`
FROM (SELECT 1 AS `anchor`) AS a
LEFT JOIN `information_schema`.`COLUMNS` AS c
  ON c.`TABLE_SCHEMA` = 'BM-sql' AND c.`TABLE_NAME` = 'workspace_snapshots'
 AND c.`COLUMN_NAME` = 'node_id' AND c.`IS_NULLABLE` = 'YES' AND c.`COLUMN_TYPE` = 'bigint unsigned'
LEFT JOIN `information_schema`.`STATISTICS` AS s
  ON s.`TABLE_SCHEMA` = 'BM-sql' AND s.`TABLE_NAME` = 'workspace_snapshots'
 AND s.`INDEX_NAME` = 'uq_workspace_snapshots_node_completion' AND s.`NON_UNIQUE` = 0
 AND s.`SEQ_IN_INDEX` = 1 AND s.`COLUMN_NAME` = 'workspace_id'
 AND (SELECT GROUP_CONCAT(`COLUMN_NAME` ORDER BY `SEQ_IN_INDEX` SEPARATOR ',')
      FROM `information_schema`.`STATISTICS`
      WHERE `TABLE_SCHEMA` = 'BM-sql' AND `TABLE_NAME` = 'workspace_snapshots'
        AND `INDEX_NAME` = 'uq_workspace_snapshots_node_completion') = 'workspace_id,node_id,snapshot_type'
LEFT JOIN `information_schema`.`KEY_COLUMN_USAGE` AS f
  ON f.`CONSTRAINT_SCHEMA` = 'BM-sql' AND f.`TABLE_NAME` = 'workspace_snapshots'
 AND f.`CONSTRAINT_NAME` = 'fk_workspace_snapshots_node' AND f.`COLUMN_NAME` = 'node_id'
 AND f.`REFERENCED_TABLE_NAME` = 'experiment_nodes' AND f.`REFERENCED_COLUMN_NAME` = 'id';

SET @task_progress_check_sql = IF(
  (SELECT COUNT(*) FROM `information_schema`.`TABLES`
   WHERE `TABLE_SCHEMA` = 'BM-sql'
     AND `TABLE_NAME` IN ('experiment_runs', 'node_tasks', 'task_progress', 'node_progress')) = 4,
  'SELECT CASE WHEN COUNT(*) = 0 THEN ''OK'' ELSE ''MISMATCH'' END AS `task_progress_status`,
          COUNT(*) AS `inconsistent_task_progress_rows`
   FROM `BM-sql`.`experiment_runs` AS `er`
   JOIN `BM-sql`.`node_tasks` AS `nt` ON `nt`.`experiment_id` = `er`.`experiment_id`
   LEFT JOIN `BM-sql`.`task_progress` AS `tp`
     ON `tp`.`run_id` = `er`.`id` AND `tp`.`experiment_id` = `er`.`experiment_id` AND `tp`.`task_id` = `nt`.`id`
   LEFT JOIN `BM-sql`.`node_progress` AS `np`
     ON `np`.`run_id` = `er`.`id` AND `np`.`experiment_id` = `er`.`experiment_id` AND `np`.`node_id` = `nt`.`node_id`
   WHERE `tp`.`id` IS NULL OR (`np`.`status` = ''completed'' AND `tp`.`status` <> ''completed'')',
  'SELECT ''SKIPPED_MISSING_SCHEMA_OR_TABLE'' AS `task_progress_status`,
          NULL AS `inconsistent_task_progress_rows`'
);
PREPARE task_progress_check_stmt FROM @task_progress_check_sql;
EXECUTE task_progress_check_stmt;
DEALLOCATE PREPARE task_progress_check_stmt;

SELECT CASE WHEN COUNT(*) = 1 THEN 'OK' ELSE 'MISMATCH' END AS `patch_applied_at_constraint_status`,
       COUNT(*) AS `constraint_count`
FROM `information_schema`.`CHECK_CONSTRAINTS` AS `cc`
WHERE `cc`.`CONSTRAINT_SCHEMA` = 'BM-sql'
  AND `cc`.`CONSTRAINT_NAME` = 'ck_code_patches_applied_at'
  AND LOWER(REPLACE(REPLACE(REPLACE(REPLACE(`cc`.`CHECK_CLAUSE`, '`', ''), ' ', ''), '(', ''), ')', ''))
      = 'status<>''applied''orapplied_atisnotnull';

SELECT CASE WHEN COUNT(*) = 1 THEN 'OK' ELSE 'MISMATCH' END AS `workspace_starting_status_constraint_status`,
       COUNT(*) AS `constraint_count`
FROM `information_schema`.`CHECK_CONSTRAINTS` AS `cc`
WHERE `cc`.`CONSTRAINT_SCHEMA` = 'BM-sql'
  AND `cc`.`CONSTRAINT_NAME` = 'ck_coding_workspaces_status'
  AND LOWER(REPLACE(REPLACE(REPLACE(REPLACE(`cc`.`CHECK_CLAUSE`, '`', ''), ' ', ''), '(', ''), ')', ''))
      = 'statusin(''provisioning'',''starting'',''running'',''stopped'',''error'',''expired'')';
