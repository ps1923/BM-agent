---
title: 'Create the BM-sql MySQL schema'
type: 'feature'
created: '2026-09-10'
status: 'done'
baseline_commit: 'NO_VCS'
context:
  - '{project-root}/README.md'
  - '{project-root}/index.html'
  - '{project-root}/app.js'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The learning-canvas prototype currently keeps experiments, folders, progress, files, and chat state only in browser memory. It needs a local MySQL database named `BM-sql` that can support multiple students writing and running code in isolated server-side workspaces.

**Approach:** Add an idempotent MySQL schema covering users, experiment definitions, student runs and progress, server workspace metadata, file indexes and snapshots, terminal executions, and agent messages; apply it to the existing local MySQL service and verify its constraints.

## Boundaries & Constraints

**Always:** Use the exact database name `BM-sql` and quote it with backticks; use `utf8mb4`; distinguish experiment definitions from per-student experiment runs; distinguish node/task definitions from per-run progress; store source code in server storage rather than database text columns; use foreign keys, uniqueness checks, and safe delete behavior; keep credentials out of repository files and command output.

**Ask First:** Request a usable MySQL administrator credential if no passwordless or saved login is available; ask before replacing incompatible existing tables, deleting data, changing the MySQL service, or installing software.

**Never:** Drop an existing database or table automatically; embed a database password in scripts; store container processes or large terminal logs directly in relational columns; implement teacher assignment, classes, courses, authentication endpoints, container orchestration, or the application backend in this change.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Fresh setup | `BM-sql` does not exist and an authorized MySQL connection is available | Database and all schema tables are created with constraints and indexes | Stop with the exact failed statement and leave prior successful DDL intact |
| Safe rerun | Database and compatible tables already exist | Script completes without deleting or duplicating data | `IF NOT EXISTS` protects existing objects |
| Name contains hyphen | Database is named `BM-sql` | Every database reference uses MySQL identifier quoting | Verification queries fail clearly if quoting is omitted |
| Existing conflict | An object exists with an incompatible definition | Existing data remains unchanged | Report the conflict; do not drop or rewrite it without approval |
| Missing credential | Local root login requires a password | No schema mutation is attempted with guessed secrets | Ask the user for a secure interactive connection method |

</frozen-after-approval>

## Code Map

- `database/schema.sql` -- Idempotent database, tables, foreign keys, checks, and indexes.
- `database/verify.sql` -- Read-only assertions for database name, required tables, and key relationships.
- `database/migrations/20260910_harden_runtime_constraints.sql` -- One-time review hardening applied to the initial cloud deployment.
- `database/deployment-report.md` -- Non-secret evidence of the cloud target and verification outcome.
- `README.md` -- Safe MySQL setup and cloud-container guidance without credentials.

## Tasks & Acceptance

**Execution:**
- [x] `database/schema.sql` -- Create the `BM-sql` schema and normalized platform tables while preserving filesystem/object storage as the source-code authority.
- [x] `database/verify.sql` -- Add read-only verification queries for expected tables, foreign keys, and character set.
- [x] `README.md` -- Document safe MySQL application and verification commands.
- [x] Target cloud MySQL instance -- Apply the schema using the server's existing authorized MySQL container and run the verification script.

**Acceptance Criteria:**
- Given an authorized connection to the target MySQL server, when `database/schema.sql` is applied, then database `BM-sql` exists with `utf8mb4` defaults and all required tables.
- Given the schema exists, when a student starts an experiment, then the model can represent one experiment definition, one student run, independent node/task progress, and one server coding workspace.
- Given a coding workspace, when files, snapshots, terminal sessions, commands, and agent messages are recorded, then every record is traceable to the owning run or workspace through foreign keys.
- Given the script is executed twice, when all existing objects are compatible, then the second execution succeeds without losing data.
- Given an experiment folder still contains experiments, when deletion is attempted, then the database rejects deletion rather than orphaning experiments.

## Spec Change Log

- 2026-09-10: The user redirected deployment from local MySQL to the configured cloud server and explicitly authorized MySQL installation. Discovery found a healthy persistent MySQL 8.0 container already serving port 3306, so it was reused to avoid a conflicting second installation. The schema and verification goals were unchanged.
- 2026-09-10: Review found that exact collation, full column/index/check/FK structure, lifecycle timestamps, workspace uniqueness, path safety, and cloud evidence were insufficiently verified. The canonical schema, read-only fingerprints, one-time migration, README, and deployment report were hardened while keeping the approved data model unchanged.

## Design Notes

Use unsigned auto-incrementing `BIGINT` identifiers for straightforward Spring Data/JPA mapping. Definition tables (`experiments`, `experiment_nodes`, `node_tasks`) remain immutable with respect to student progress, while run-state tables (`experiment_runs`, `node_progress`, `task_progress`) isolate each learner. Runtime containers are disposable; `coding_workspaces.storage_path` and `workspace_snapshots.storage_key` identify durable storage. `workspace_files` is an index only and intentionally has no source-content column.

## Verification

**Commands:**
- `mysql --user=<authorized-user> --password < database/schema.sql` -- expected: exits successfully without printing credentials.
- `mysql --user=<authorized-user> --password < database/verify.sql` -- expected: reports the server version, `BM-sql`, every required table, and exact structure fingerprints.
- `mysql --user=<authorized-user> --password -N -e "SELECT DEFAULT_CHARACTER_SET_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='BM-sql';"` -- expected: `utf8mb4`.

## Suggested Review Order

**Data model**

- Start with the experiment definition and its ownership boundaries.
  [`schema.sql:40`](../../database/schema.sql#L40)

- Follow the per-student run and progress separation.
  [`schema.sql:148`](../../database/schema.sql#L148)

- Inspect isolated runtime identity and durable storage metadata.
  [`schema.sql:244`](../../database/schema.sql#L244)

- Check filesystem-path safety and case-sensitive uniqueness.
  [`schema.sql:275`](../../database/schema.sql#L275)

**Verification and deployment**

- Review version, collation, table, and foreign-key checks.
  [`verify.sql:5`](../../database/verify.sql#L5)

- Inspect full structure fingerprints used to detect schema drift.
  [`verify.sql:112`](../../database/verify.sql#L112)

- Review the one-time hardening applied after adversarial review.
  [`20260910_harden_runtime_constraints.sql:6`](../../database/migrations/20260910_harden_runtime_constraints.sql#L6)

- Confirm the non-secret cloud deployment evidence and residual risk.
  [`deployment-report.md:1`](../../database/deployment-report.md#L1)

- Finish with portable setup and operational guidance.
  [`README.md:18`](../../README.md#L18)
