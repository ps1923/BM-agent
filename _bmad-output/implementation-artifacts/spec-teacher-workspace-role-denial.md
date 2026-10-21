---
title: 'Verify Teacher Denial on Student Workspace Progress APIs'
type: 'chore'
created: '2026-10-03'
status: 'done'
baseline_commit: 'c213781f4ddb5401cd303d78660591d60ef495c3'
context:
  - '{project-root}/backend/src/main/java/com/bmhs/workspace/WorkspaceController.java'
  - '{project-root}/backend/src/main/java/com/bmhs/workspace/WorkspaceService.java'
  - '{project-root}/backend/src/test/java/com/bmhs/workspace/WorkspaceControllerWebTest.java'
---

<frozen-after-approval reason="continuation of project security work order">

## Intent

**Problem:** Workspace progress routes are intended for students, but the existing HTTP tests do not prove that an authenticated teacher is denied before repository access.

**Approach:** Add controller-level MockMvc regression coverage for teacher requests to the node and task progress endpoints. Preserve the existing service role guard and assert both the stable forbidden response and zero repository interactions.

## Boundaries & Constraints

**Always:** Exercise the real controller, session authentication filter, argument resolver, validation, and exception handler in the test harness; verify that denied requests do not interact with the repository at all.

**Ask First:** Any change to role policy, response contract, authentication design, or production service/controller behavior.

**Never:** Do not alter authorization behavior merely to satisfy tests; do not access a live database or server.

</frozen-after-approval>

## Code Map

- `backend/src/main/java/com/bmhs/workspace/WorkspaceService.java` -- existing student-role guard expected to reject teachers before repository access.
- `backend/src/test/java/com/bmhs/workspace/WorkspaceControllerWebTest.java` -- authenticated HTTP-level regression coverage.

## Tasks & Acceptance

**Execution:**
- [x] Add teacher-session requests for node and task progress updates and assert rejection before repository access.

**Acceptance Criteria:**
- Given an authenticated teacher, when they PATCH a student's node or task progress endpoint, then each request returns HTTP 403 with `ROLE_FORBIDDEN`.
- Given either denied request, when repository interactions are inspected, then the repository has zero interactions.

## Verification

- [x] Focused `WorkspaceControllerWebTest`: 6/6 passed.
- [x] Full Spring Boot suite: 130/130 passed with cached Maven 3.9.9; opt-in MySQL test not run.
- [x] `git diff --check` passed.

## Suggested Review Order

- Start with the two authenticated REST requests and their zero-repository-interaction assertion.
  [`WorkspaceControllerWebTest.java:140`](../../backend/src/test/java/com/bmhs/workspace/WorkspaceControllerWebTest.java#L140)
- Confirm the pre-existing student-role gate remains ahead of repository access.
  [`WorkspaceService.java:1365`](../../backend/src/main/java/com/bmhs/workspace/WorkspaceService.java#L1365)
