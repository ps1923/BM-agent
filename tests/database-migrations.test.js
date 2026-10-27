import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const read = (path) => readFile(new URL(path, import.meta.url), 'utf8');

test('task-progress backfill covers runs even when a node_progress row is missing', async () => {
  const sql = await read('../database/migrations/20261001_backfill_task_progress.sql');
  assert.match(sql, /INSERT INTO `task_progress`/);
  assert.match(sql, /FROM `experiment_runs` AS `er`/);
  assert.match(sql, /JOIN `node_tasks` AS `nt`/);
  assert.match(sql, /LEFT JOIN `node_progress` AS `np`/);
  assert.match(sql, /ON DUPLICATE KEY UPDATE/);
  assert.match(sql, /VALUES\(`status`\) = 'completed'/);
  assert.match(sql, /'completed', `task_progress`\.`status`/);
  assert.match(sql, /COALESCE\(`task_progress`\.`completed_at`, VALUES\(`completed_at`\)\)/);
  assert.match(sql, /COALESCE\(`np`\.`completed_at`, `np`\.`updated_at`, CURRENT_TIMESTAMP\(6\)\)/);
});

test('constraint migrations can be rerun and preserve valid rejected patches', async () => {
  const [patches, workspace] = await Promise.all([
    read('../database/migrations/20261001_fix_rejected_patch_timestamp_constraint.sql'),
    read('../database/migrations/20261001_add_workspace_starting_state.sql'),
  ]);
  assert.match(patches, /CHECK \(`status` <> 'applied' OR `applied_at` IS NOT NULL\)/);
  assert.match(patches, /information_schema`\.`CHECK_CONSTRAINTS/);
  assert.match(workspace, /'starting'/);
  assert.match(workspace, /information_schema`\.`CHECK_CONSTRAINTS/);
  assert.doesNotMatch(`${patches}\n${workspace}`, /DROP\s+TABLE|TRUNCATE\s+TABLE|DELETE\s+FROM/i);
});

test('terminal command identity migration is additive, nullable for legacy rows, and uniquely session scoped', async () => {
  const [migration, schema, verifier] = await Promise.all([
    read('../database/migrations/20261003_add_terminal_command_idempotency.sql'),
    read('../database/schema.sql'),
    read('../database/verify.sql'),
  ]);
  assert.match(migration, /ADD COLUMN `client_command_id` CHAR\(36\) NULL/);
  assert.match(migration, /ADD UNIQUE KEY `uq_command_runs_session_client_id` \(`terminal_session_id`, `client_command_id`\)/);
  assert.match(migration, /information_schema`\.`COLUMNS/);
  assert.match(migration, /information_schema`\.`STATISTICS/);
  assert.doesNotMatch(migration, /DROP\s+TABLE|TRUNCATE\s+TABLE|DELETE\s+FROM|UPDATE\s+`command_runs`/i);
  assert.match(schema, /`client_command_id` CHAR\(36\) NULL/);
  assert.match(schema, /UNIQUE KEY `uq_command_runs_session_client_id` \(`terminal_session_id`, `client_command_id`\)/);
  assert.match(verifier, /'command_runs', 'client_command_id', 'char\(36\)', 'YES'/);
  assert.match(verifier, /'command_runs', 'uq_command_runs_session_client_id', 0/);
  assert.match(verifier, /terminal_session_id,client_command_id/);
  assert.match(verifier, /NOT \(`TABLE_NAME` = 'command_runs' AND `COLUMN_NAME` = 'client_command_id'\)/);
  assert.match(verifier, /NOT \(`TABLE_NAME` = 'command_runs' AND `INDEX_NAME` = 'uq_command_runs_session_client_id'\)/);
});

test('terminal session identity migration is additive, nullable for legacy rows, and uniquely workspace scoped', async () => {
  const [migration, schema, verifier] = await Promise.all([
    read('../database/migrations/20261005_add_terminal_session_idempotency.sql'),
    read('../database/schema.sql'),
    read('../database/verify.sql'),
  ]);
  assert.match(migration, /ADD COLUMN `client_session_id` CHAR\(36\) NULL/);
  assert.match(migration, /ADD UNIQUE KEY `uq_terminal_sessions_workspace_client_id` \(`workspace_id`, `client_session_id`\)/);
  assert.match(migration, /information_schema`\.`COLUMNS/);
  assert.match(migration, /information_schema`\.`STATISTICS/);
  assert.doesNotMatch(migration, /DROP\s+TABLE|TRUNCATE\s+TABLE|DELETE\s+FROM|UPDATE\s+`terminal_sessions`/i);
  assert.match(schema, /`client_session_id` CHAR\(36\) NULL/);
  assert.match(schema, /UNIQUE KEY `uq_terminal_sessions_workspace_client_id` \(`workspace_id`, `client_session_id`\)/);
  assert.match(verifier, /'terminal_sessions', 'client_session_id', 'char\(36\)', 'YES'/);
  assert.match(verifier, /'terminal_sessions', 'uq_terminal_sessions_workspace_client_id', 0/);
  assert.match(verifier, /workspace_id,client_session_id/);
  assert.match(verifier, /NOT \(`TABLE_NAME` = 'terminal_sessions' AND `COLUMN_NAME` = 'client_session_id'\)/);
  assert.match(verifier, /NOT \(`TABLE_NAME` = 'terminal_sessions' AND `INDEX_NAME` = 'uq_terminal_sessions_workspace_client_id'\)/);
});

test('database verifier checks exact patch and workspace status predicates', async () => {
  const sql = await read('../database/verify.sql');
  assert.match(sql, /LOWER\(REGEXP_REPLACE\(/);
  assert.match(sql, /REGEXP_LIKE\(actual\.`CHECK_CLAUSE`, expected\.`literal_pattern`, 'cn'\)/);
  assert.match(sql, /REGEXP_LIKE\(`cc`\.`CHECK_CLAUSE`/);
  const enforcementQueryStart = sql.indexOf("'NOT_ENFORCED' AS `enforcement_status`");
  const enforcementQuery = sql.slice(sql.lastIndexOf('\nSELECT', enforcementQueryStart), sql.indexOf('\nSET SESSION', enforcementQueryStart));
  assert.match(enforcementQuery, /SELECT tc\.`TABLE_NAME`, tc\.`CONSTRAINT_NAME`, tc\.`ENFORCED`/);
  assert.match(enforcementQuery, /FROM `information_schema`\.`TABLE_CONSTRAINTS` AS tc/);
  assert.doesNotMatch(enforcementQuery, /\bcc\./);
  assert.match(sql, /'\.\*''applied''\.\*', 'cn'\)/);
  assert.match(sql, /'\.\*''provisioning''\.\*''starting''/);
  assert.doesNotMatch(sql, /(?:WHEN|AND) BINARY REGEXP_REPLACE\(/);
  assert.ok(sql.includes("'[[:space:]]+(?=(?:[^'']*''[^'']*'')*[^'']*$)'"));
  assert.ok(sql.includes("'_utf8mb4(?='')(?=(?:[^'']*''[^'']*'')*[^'']*$)'"));
  assert.ok(sql.includes("'`(?=(?:[^'']*''[^'']*'')*[^'']*$)'"));
  assert.doesNotMatch(sql, /REPLACE\([^,\r\n]+,\s*'\(',\s*''/);
  assert.match(sql, /patch_applied_at_constraint_status/);
  assert.match(sql, /workspace_starting_status_constraint_status/);
  assert.match(sql, /course_invite_index_columns_status/);
  assert.match(sql, /uq_course_experiment_assignment/);
  assert.match(sql, /session_token_index_columns_status/);
  assert.match(sql, /fk_bug_cases_run/);
  assert.match(sql, /referenced_column_list/);
  assert.match(sql, /ck_bug_case_reviews_decision/);
  assert.match(sql, /inconsistent_task_progress_rows/);
  assert.match(sql, /ENFORCED/);
  assert.match(sql, /SKIPPED_MISSING_SCHEMA_OR_TABLE/);
  assert.match(sql, /`BM-sql`\.`experiment_runs`/);
  assert.match(sql, /'\(\(status<>''applied''\)or\(applied_atisnotnull\)\)'/);
  assert.match(sql, /'\(statusin\(''provisioning'',''starting''/);
});

test('DSH baseline migration defines tables once and matches fresh opaque bindings and personal review schema', async () => {
  const [migration, schema, verifier] = await Promise.all([
    read('../database/migrations/20261021_add_dsh_plugin_baseline.sql'),
    read('../database/schema.sql'),
    read('../database/verify.sql'),
  ]);
  for (const table of ['experiment_reviewers', 'dsh_instances', 'dsh_access_tokens', 'dsh_refresh_tokens', 'dsh_run_bindings', 'code_submissions', 'code_submission_files']) {
    const marker = `CREATE TABLE IF NOT EXISTS \`${table}\``;
    assert.equal(migration.split(marker).length - 1, 1, `${table} is defined once in the migration`);
    assert.ok(schema.includes(marker), `${table} is present in the fresh schema`);
  }
  assert.match(migration, /KEY `idx_dsh_run_binding_refs` \(`instance_id`, `workspace_ref`, `session_ref`\)/);
  assert.match(schema, /KEY `idx_dsh_run_binding_refs` \(`instance_id`, `workspace_ref`, `session_ref`\)/);
  assert.match(verifier, /'dsh_instances'/);
  assert.match(verifier, /'dsh_refresh_tokens'/);
  assert.match(migration, /`route_key` CHAR\(36\) NOT NULL/);
  assert.match(migration, /`proxy_port` SMALLINT UNSIGNED NULL/);
  assert.match(migration, /'stopping'/);
  assert.match(verifier, /dsh_execution_mode_column_status/);
  assert.match(verifier, /dsh_opaque_binding_index_status/);
  assert.match(migration, /MODIFY COLUMN `course_id` BIGINT UNSIGNED NULL/);
  assert.match(schema, /CREATE TABLE IF NOT EXISTS `teacher_reviews` \([\s\S]*?`course_id` BIGINT UNSIGNED NULL/);
  assert.match(verifier, /dsh_teacher_review_course_nullable_status/);
  assert.doesNotMatch(migration, /DROP\s+TABLE|TRUNCATE\s+TABLE|DELETE\s+FROM/i);
});

test('Git commit acceptance migration is additive, experiment-scoped, and preserves legacy progress', async () => {
  const [migration, schema, verifier] = await Promise.all([
    read('../database/migrations/20261022_add_git_commit_acceptance.sql'),
    read('../database/schema.sql'),
    read('../database/verify.sql'),
  ]);
  for (const table of ['git_repositories', 'git_submissions', 'git_submission_nodes', 'git_acceptance_results']) {
    const marker = `CREATE TABLE IF NOT EXISTS \`${table}\``;
    assert.equal(migration.split(marker).length - 1, 1, `${table} is defined once in the migration`);
    assert.ok(schema.includes(marker), `${table} is present in the fresh schema`);
    assert.ok(verifier.includes(`'${table}'`), `${table} is checked by the verifier`);
  }
  assert.match(migration, /UNIQUE KEY `uq_git_submissions_run_sha` \(`run_id`, `commit_sha`\)/);
  assert.match(migration, /FOREIGN KEY \(`node_id`, `experiment_id`\)\s+REFERENCES `experiment_nodes`/);
  assert.match(migration, /CHECK \(`decision` IN \('meets', 'partially_meets', 'does_not_meet', 'unable_to_verify'\)\)/);
  assert.match(migration, /r\.execution_mode = 'git'/);
  assert.doesNotMatch(migration, /execution_mode IN \('dsh', 'git'\)|DROP\s+TABLE|TRUNCATE\s+TABLE|DELETE\s+FROM/i);
});

test('teacher role and personal center migration preserves accounts and assignment data', async () => {
  const migration = await read('../database/migrations/20261023_teacher_role_personal_center_course_git.sql');
  const [schema, baseline, verifier] = await Promise.all([
    read('../database/schema.sql'),
    read('../database/migrations/20261021_add_dsh_plugin_baseline.sql'),
    read('../database/verify.sql'),
  ]);
  assert.match(migration, /UPDATE `users` SET `role` = 'teacher' WHERE `role` = 'admin'/);
  assert.match(migration, /ADD COLUMN `course_id` BIGINT UNSIGNED NULL/);
  assert.match(migration, /SELECT MIN\(c\.id\)[\s\S]+WHERE e\.id = er\.experiment_id/);
  assert.match(migration, /ADD COLUMN `assigned_by_teacher_id` BIGINT UNSIGNED NULL/);
  assert.match(migration, /assigned_by_teacher_id` = `assigned_by_admin_id` WHERE `assigned_by_teacher_id` IS NULL/);
  assert.match(migration, /MODIFY COLUMN `assigned_by_admin_id` BIGINT UNSIGNED NULL/);
  assert.doesNotMatch(migration, /DROP COLUMN `assigned_by_admin_id`|DROP FOREIGN KEY `fk_experiment_reviewers_admin`/);
  assert.match(migration, /CHECK \(`role` IN \(''student'', ''teacher''\)\)/);
  assert.match(migration, /information_schema\.COLUMNS/);
  assert.match(migration, /MODIFY COLUMN `assigned_by_teacher_id` BIGINT UNSIGNED NOT NULL/);
  assert.match(migration, /git_author_name/);
  assert.match(migration, /git_author_email/);
  assert.match(migration, /git_repository_url/);
  assert.doesNotMatch(migration, /DROP\s+TABLE|TRUNCATE\s+TABLE|DELETE\s+FROM/i);
  assert.match(schema, /CHECK \(`role` IN \('student', 'teacher'\)\)/);
  assert.match(schema, /`assigned_by_teacher_id` BIGINT UNSIGNED NOT NULL/);
  assert.match(baseline, /`assigned_by_admin_id` BIGINT UNSIGNED NOT NULL/);
  assert.match(baseline, /`fk_experiment_reviewers_admin`/);
  assert.match(verifier, /'users', 'git_author_name', 'varchar\(100\)', 'YES'/);
  assert.match(verifier, /'courses', 'git_repository_url', 'varchar\(1000\)', 'YES'/);
  assert.match(verifier, /fk_experiment_reviewers_assigned_by_teacher/);
});

test('node acceptance criteria migration is additive and stores immutable report snapshots', async () => {
  const migration = await read('../database/migrations/20261027_node_acceptance_criteria.sql');
  const repository = await read('../backend/src/main/java/com/bmhs/git/GitSubmissionRepository.java');
  const [schema, verifier] = await Promise.all([
    read('../database/schema.sql'),
    read('../database/verify.sql'),
  ]);
  assert.match(migration, /CREATE TABLE IF NOT EXISTS `experiment_node_acceptance_criteria`/);
  assert.match(migration, /UNIQUE KEY `uq_node_acceptance_criteria_version` \(`node_id`, `version`\)/);
  assert.match(migration, /FOREIGN KEY \(`node_id`, `experiment_id`\)[\s\S]+REFERENCES `experiment_nodes`/);
  assert.match(migration, /USE `BM-sql`/);
  for (const column of ['criteria_version', 'criteria_source', 'criteria_snapshot_json']) {
    assert.match(migration, new RegExp(`ADD COLUMN \\x60${column}\\x60`));
    assert.match(schema, new RegExp(`\\x60${column}\\x60`));
    assert.match(verifier, new RegExp(`'git_submission_nodes', '${column}'`));
  }
  assert.match(repository, /INSERT INTO git_submission_nodes[\s\S]+criteria_snapshot_json[\s\S]+criteriaGson\.toJson\(snapshot\.criteria\(\)\)/);
  assert.match(repository, /sn\.criteria_snapshot_json AS criteria_json|COALESCE\(sn\.criteria_snapshot_json, c\.criteria_json\)/);
  for (const column of ['criteria_version', 'criteria_source', 'criteria_snapshot_json', 'criterion_results_json']) {
    assert.match(migration, new RegExp(`ADD COLUMN \\x60${column}\\x60`));
    assert.match(schema, new RegExp(`\\x60${column}\\x60`));
    assert.match(verifier, new RegExp(`'git_acceptance_results', '${column}'`));
  }
  assert.match(verifier, /fk_node_acceptance_criteria_node/);
  assert.match(verifier, /fk_node_acceptance_criteria_editor/);
  assert.match(verifier, /ck_node_acceptance_criteria_source/);
  assert.doesNotMatch(migration, /DROP\s+TABLE|TRUNCATE\s+TABLE|DELETE\s+FROM/i);
});

test('RAG index retry migration is additive and bounds scheduled retry state', async () => {
  const migration = await read('../database/migrations/20261027_rag_index_retry.sql');
  const [schema, verifier] = await Promise.all([
    read('../database/schema.sql'),
    read('../database/verify.sql'),
  ]);
  for (const column of ['vector_index_attempts', 'vector_index_retry_at']) {
    assert.match(migration, new RegExp(`ADD COLUMN \\x60${column}\\x60`));
    assert.match(schema, new RegExp(`\\x60${column}\\x60`));
    assert.match(verifier, new RegExp(`'bug_cases', '${column}'`));
  }
  assert.match(migration, /CREATE INDEX `idx_bug_cases_vector_retry`/);
  assert.match(migration, /USE `BM-sql`/);
  assert.match(migration, /ck_bug_cases_vector_attempts/);
  assert.match(schema, /CONSTRAINT `ck_bug_cases_vector_attempts` CHECK/);
  assert.match(verifier, /ck_bug_cases_vector_attempts/);
  assert.doesNotMatch(migration, /DROP\s+TABLE|TRUNCATE\s+TABLE|DELETE\s+FROM/i);
});

test('fresh acceptance demo seed creates isolated teacher, student, course, and a 3-level experiment', async () => {
  const seed = await read('../database/demo_seed_acceptance.sql');
  assert.match(seed, /teacher\.demo@example\.com/);
  assert.match(seed, /student\.demo@example\.com/);
  assert.match(seed, /BMHSDEMO26/);
  assert.match(seed, /bmhs-node-acceptance-demo-v1/);
  assert.match(seed, /'1\.1\.1'/);
  assert.match(seed, /experiment_node_acceptance_criteria/);
  assert.match(seed, /verification_method/);
  assert.match(seed, /expected_evidence/);
  assert.match(seed, /WHERE NOT EXISTS/);
  assert.doesNotMatch(seed, /DROP\s+TABLE|TRUNCATE\s+TABLE|DELETE\s+FROM/i);
});

test('check-clause normalization removes MySQL formatting but preserves logical grouping', () => {
  const normalize = (clause) => {
    const source = clause;
    let result = '';
    let quoted = false;
    for (let index = 0; index < source.length; index += 1) {
      const character = source[index];
      if (character === String.fromCharCode(92) && source[index + 1] === "'") {
        const afterEscapedQuote = source[index + 2];
        if (!quoted) quoted = true;
        else if (!/[\p{L}\p{N}_]/u.test(afterEscapedQuote || '')) quoted = false;
        result += "'";
        index += 1;
      } else if (character === "'") {
        quoted = !quoted;
        result += character;
      } else if (quoted) {
        result += character;
      } else if (/\s|`/.test(character)) {
        continue;
      } else if (source.slice(index, index + 8).toLowerCase() === '_utf8mb4') {
        index += 7;
      } else {
        result += character.toLowerCase();
      }
    }
    return result;
  };
  const mysqlClause = "((`status` <> _utf8mb4\\'applied\\')\n\tor ((`applied_at` is not null)\tand (`reviewed_at` is not null)))";
  const expectedClause = "((status <> 'applied') or ((applied_at is not null) and (reviewed_at is not null)))";
  const differentlyGroupedClause = "((status <> 'applied' or applied_at is not null) and reviewed_at is not null)";
  const literalWithSpaces = "status in ('in progress')";
  const changedLiteralWithoutSpaces = "status in ('inprogress')";
  const charsetTextInsideLiteral = "status = '_utf8mb4'";
  const literalWithBackslash = "status = 'path\\\\name'";
  const literalWithEscapedApostrophe = "message = 'student\\'s work'";
  const changedLiteralCase = "STATUS = 'APPLIED'";
  const changedKeywordCase = "STATUS = 'applied'";

  assert.equal(normalize(mysqlClause), normalize(expectedClause));
  assert.notEqual(normalize(mysqlClause), normalize(differentlyGroupedClause));
  assert.notEqual(normalize(literalWithSpaces), normalize(changedLiteralWithoutSpaces));
  assert.notEqual(normalize(changedLiteralCase), normalize("status = 'applied'"));
  assert.equal(normalize(changedKeywordCase), normalize("status = 'applied'"));
  assert.equal(normalize(charsetTextInsideLiteral), "status='_utf8mb4'");
  assert.equal(normalize(literalWithBackslash), "status='path\\\\name'");
  assert.equal(normalize(literalWithEscapedApostrophe), "message='student's work'");
});
