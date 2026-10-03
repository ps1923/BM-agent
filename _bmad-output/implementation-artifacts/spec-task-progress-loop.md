---
title: 'Task Progress and Node Completion Loop'
type: 'feature'
created: '2026-10-01'
status: 'in-review'
baseline_commit: '8454527b846d8add4f39178501c9fa81ac7f91a0'
context:
  - '{project-root}/_bmad-output/implementation-artifacts/spec-create-bm-sql-schema.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The student editor presents task rows with check-like controls, but they only change selection; task completion is not persisted. The node completion endpoint can mark a node complete without checking its declared tasks, and teacher progress reports nodes only. This contradicts the plan's `all_tasks_completed` rule and makes progress evidence misleading.

**Approach:** Complete the existing task-progress model across run creation, student APIs/UI, node completion validation, and teacher progress views. Students explicitly record task completion; the server remains authoritative, and a node with declared tasks cannot be completed until all of them are complete.

## Boundaries & Constraints

**Always:** Preserve existing uncommitted work; use the existing `task_progress` table and status model where compatible; scope every task read/write to the authenticated student's run and experiment; persist progress across refreshes; keep validation honest as student-confirmed rather than machine-verified; keep node and run counters consistent; use additive, data-preserving database changes.

**Ask First:** Any production deployment, destructive migration, automatic code-based task verification, or change to the experiment/task completion rules beyond `all_tasks_completed`.

**Never:** Do not drop or reset user data; do not let a client-supplied task/node ID cross run or experiment boundaries; do not allow completion of a declared-task node while a task is incomplete; do not claim AI or server verification for a student's self-confirmation.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Mark task done | Authorized student marks a pending task complete | Task state persists; returned run progress reflects it | Return scoped not-found/forbidden response for another run's task |
| Complete node early | Any declared task is not completed | Node stays unchanged; UI keeps completion disabled | 409 with stable `NODE_TASKS_INCOMPLETE` code |
| All tasks done | Every declared task is complete | Student may complete node; child unlocks and teacher counters update | Transaction rollback leaves both task and node state consistent |
| Reopen / locked node | Student changes a task on a locked node or on a completed node | No task state is changed | Return 409 with a stable state-conflict code |
| Refresh / existing run | Progress exists or a run predates task tracking | Current task states render accurately; missing rows are initialized idempotently without overwriting saved states | Surface load failure and allow retry; do not silently show all complete |

</frozen-after-approval>

## Code Map

- `database/schema.sql`, `database/verify.sql`, `database/migrations/` -- task-progress schema, integrity checks, and additive deployment compatibility.
- `backend/src/main/java/com/bmhs/workspace/WorkspaceRepository.java` -- run progress initialization, reads/writes, and node completion transaction.
- `backend/src/main/java/com/bmhs/workspace/WorkspaceModels.java`, `WorkspaceController.java`, `WorkspaceService.java` -- API contracts, validation, and student ownership checks.
- `backend/src/main/java/com/bmhs/course/CourseRepository.java`, `backend/src/main/java/com/bmhs/experimentcreation/CreationModels.java` -- include stable task IDs in materialized experiment data without changing model-generated task input.
- `backend/src/main/java/com/bmhs/teacher/TeacherRepository.java`, `TeacherModels.java`, `app.js` -- aggregate and display task progress in teacher monitoring and run detail.
- `app.js`, `index.html`, `styles.css` -- accessible persisted task controls and truthful node-completion state.
- `backend/src/test/java/com/bmhs/workspace/`, `backend/src/test/java/com/bmhs/teacher/`, `tests/` -- boundary, persistence, contract, and UI-facing regression coverage.

## Tasks & Acceptance

**Execution:**
- [x] `backend/src/main/java/com/bmhs/workspace/` -- add authenticated task-progress read/update behavior and atomically gate node completion -- enforce `all_tasks_completed` on the server.
- [x] `backend/src/main/java/com/bmhs/course/CourseRepository.java`, `backend/src/main/java/com/bmhs/experimentcreation/CreationModels.java` -- expose stable task IDs only in materialized experiment output -- let the UI address persisted task rows without altering generated-plan contracts.
- [x] `database/` -- verify/add compatible additive migration and update schema checks -- support both existing and fresh databases without losing progress.
- [x] `app.js`, `index.html`, `styles.css` -- render accessible completion controls, progress/loading/error feedback, and teacher task counts -- make visible state match persisted state.
- [ ] `backend/src/test/java/com/bmhs/workspace/`, `backend/src/test/java/com/bmhs/teacher/`, `tests/` -- add behavior-level coverage for persistence, empty-task nodes, full completion/unlock, isolation, and teacher aggregates -- prevent regressions at API and UI boundaries.

**Acceptance Criteria:**
- Given a student opens a run, when task progress loads, then each task shows its saved state and a stable accessible control.
- Given a student marks a task complete and reloads the run, when progress is fetched again, then the same task remains completed.
- Given any declared task is incomplete, when the student requests node completion directly through REST, then the server rejects it without changing node, run, or child-node state.
- Given all tasks are complete, when the student completes the node, then node progress, child unlocking, and run state update atomically.
- Given a teacher opens an authorized course dashboard or run detail, when student progress is displayed, then completed/total task counts agree with persisted task rows and existing node counts.
- Given a student requests or updates another run's task, when the request is processed, then no data is exposed or changed.
- Given a node declares no tasks, when the student completes it, then the existing manual node-completion flow remains available.

## Spec Change Log

## Design Notes

- `task_progress.status = completed` records student confirmation only. The current release does not execute validators or infer completion from editor contents; `validation_result` remains reserved for a future, separately approved feature.
- Treat the task list as definition data with immutable task IDs. Progress is keyed by `(run_id, task_id)`; GET-time backfill, if needed for old runs, must be idempotent and must never overwrite existing status.
- For legacy runs with a completed node but no task rows, seed that node's tasks as completed using its original `completed_at`: the earlier explicit node-completion action is treated as the student's overall self-attestation, never as machine verification. Other legacy tasks start pending.
- Keep `GET /api/runs/{runId}/progress` as the single progress read contract and include task IDs, node IDs, status, and completion time. Add `PATCH /api/runs/{runId}/tasks/{taskId}` accepting only `pending` or `completed`; derive node percentage from completed/total tasks, but preserve the explicit “complete node” action after all tasks are checked. Include completed/total task aggregates in teacher dashboard and run summary without multiplying existing node or activity counts.

## Verification

**Commands:**
- `cd backend && mvn test` -- expected: all tests pass, including task ownership and completion-gate cases.
- `node --check app.js` -- expected: no syntax errors.
- `node --test tests/*.test.js` -- expected: all browser/API contract tests pass.
- `git diff --check` -- expected: no whitespace errors.

**Manual checks:**
- In a real student browser session, complete one task, refresh, verify it remains checked, and verify node completion remains disabled until all declared tasks are complete.
- Attempt direct node completion while a task is incomplete and confirm no progress changes; complete all tasks and verify next-node unlocking.
- In the owning teacher's course dashboard and run detail, verify task counts and node counts match the student view; verify a different course cannot see the run.

**Historical verification result (superseded by the 2026-10-01 stage-code-evidence audit):**
- Backend suite: 86 tests passed, including materialized task-ID replay, server-derived node percentage, empty-task completion/unlock, and progress-read contract cases; frontend syntax check and 7 API configuration tests passed; Compose configuration validation passed; `git diff --check` reported no whitespace errors (only existing LF/CRLF notices).
- Browser inspection confirmed persisted-task controls, disabled node completion when task progress is unavailable, and a visible retry action when the API returns the legacy progress shape without `tasks`.
- Database-backed persistence, task completion across refreshes, actual child unlock behavior, teacher aggregate SQL, and course isolation remain unverified: the local API at `127.0.0.1:8080` is an older Node service returning the old progress contract; Docker Engine is unavailable, and the local MySQL service may contain user data. No server or database changes were made.

## Suggested Review Order

**Server-side progress integrity**

- Start with the transaction and task-state gate that make node completion authoritative.
  [`WorkspaceRepository.java:156`](../../backend/src/main/java/com/bmhs/workspace/WorkspaceRepository.java#L156)

- Follow task ownership, locking, and server-derived progress through updates.
  [`WorkspaceRepository.java:182`](../../backend/src/main/java/com/bmhs/workspace/WorkspaceRepository.java#L182)

**Student interaction and recovery**

- Inspect accessible task state, missing-data handling, retry, and the completion-button gate.
  [`app.js:918`](../../app.js#L918)

- Trace progress response validation and refresh recovery for existing runs.
  [`app.js:1247`](../../app.js#L1247)

**Persisted task identity and teacher reporting**

- Verify experiment creation returns task identifiers read back from persisted definitions.
  [`ExperimentCreationService.java:131`](../../backend/src/main/java/com/bmhs/experimentcreation/ExperimentCreationService.java#L131)

- Check that teacher task totals are aggregated separately from node counts.
  [`TeacherRepository.java:28`](../../backend/src/main/java/com/bmhs/teacher/TeacherRepository.java#L28)

**Migration and regression evidence**

- Review the additive, idempotent backfill for existing student runs.
  [`20261001_backfill_task_progress.sql:5`](../../database/migrations/20261001_backfill_task_progress.sql#L5)

- Finish with materialization identity and task ownership regression cases.
  [`ExperimentCreationServiceTest.java:119`](../../backend/src/test/java/com/bmhs/experimentcreation/ExperimentCreationServiceTest.java#L119)
