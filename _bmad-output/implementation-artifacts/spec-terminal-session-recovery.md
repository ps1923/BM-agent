---
title: 'Recover Stale Browser-Cached Terminal Sessions'
type: 'bugfix'
created: '2026-10-03'
status: 'in-review'
baseline_commit: 'c213781f4ddb5401cd303d78660591d60ef495c3'
context:
  - '{project-root}/app.js'
  - '{project-root}/styles.css'
  - '{project-root}/tests/terminal-session-recovery.test.js'
---

<frozen-after-approval reason="project-goal work order">

## Intent

**Problem:** A terminal session may be closed server-side while the browser still has an `open` session cached for the run. A command then returns `TERMINAL_SESSION_CLOSED`, and the page keeps reusing the stale session.

**Approach:** On that exact structured API error only, create and validate a replacement session for the same authorized workspace, persist it best-effort, and tell the student the failed command was not replayed and must be deliberately retried. All unrelated errors keep their existing behavior.

## Boundaries & Constraints

**Always:** Preserve current-run, workspace, and session identity checks; only recover the explicit `TERMINAL_SESSION_CLOSED` error; never replay a command automatically; validate replacement session ID, status, and workspace before adoption.

**Ask First:** Backend terminal lifecycle, authorization rules, or terminal API contract changes.

**Never:** Do not automatically repeat shell commands after recovery; do not treat generic 409, 404, timeout, network, or workspace errors as a closed-session signal.

</frozen-after-approval>

## Acceptance Criteria

- Given the server explicitly reports `TERMINAL_SESSION_CLOSED`, when a command fails, then the client requests one replacement for the same workspace, adopts it only if its identity is valid, and tells the student the command was not replayed.
- Given any other command error, when it occurs, then no replacement session is created.
- Given the route/run/session changes while recovery is pending, when the replacement arrives, then it cannot overwrite the newer session or alter its controls.
- Given replacement creation fails or returns an invalid session, then the original failure remains visible, terminal commands stay disabled, and an explicit retry control creates a valid session and re-enables the terminal without replaying the failed command.

## Verification

- [x] `app.js` preserves API error codes and recovers only the explicit closed-session response without replaying the command.
- [x] `tests/terminal-session-recovery.test.js` executes the production command handler and covers explicit closure, no automatic replay, unrelated/malformed responses, replacement retry, invalid IDs/workspaces/status, cache validation, lock isolation, stale route/session/workspace responses, file-refresh failure cleanup, and exact shell command preservation.
- [x] Focused terminal regression tests: 22/22.
- [x] Full `node --test tests/*.test.js`: 77/77 frontend/API tests.
- [x] Backend Spring Boot unit/Web tests: 129/129 with cached Maven 3.9.9; opt-in MySQL integration test was not run.
- [x] JavaScript syntax checks and `git diff --check` passed.
- Actual terminal page interaction remains subject to availability of the Spring Boot API and Docker-backed workspace.
