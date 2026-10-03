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

test('database verifier checks exact patch and workspace status predicates', async () => {
  const sql = await read('../database/verify.sql');
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
  assert.match(sql, /'status<>''applied''orapplied_atisnotnull'/);
  assert.match(sql, /'statusin\(''provisioning'',''starting''/);
});
