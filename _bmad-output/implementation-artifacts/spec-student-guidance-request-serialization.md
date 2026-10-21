---
title: 'Serialize Student Guidance Requests Per Run'
type: 'bugfix'
created: '2026-10-03'
status: 'in-review'
baseline_commit: 'c213781f4ddb5401cd303d78660591d60ef495c3'
context:
  - '{project-root}/app.js'
  - '{project-root}/index.html'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The guidance buttons are disabled while a request runs, but Ctrl/Cmd+Enter calls the same submit path without checking that state. A student can therefore issue overlapping requests, producing duplicated work and assistant replies that arrive out of order.

**Approach:** Enforce one in-flight ordinary guidance request per run at the request boundary, not only at the button. Keep typed drafts when a duplicate keyboard submission is rejected, and derive button availability from the currently selected run so late responses cannot lock or unlock another run's controls.

## Boundaries & Constraints

**Always:** Allow at most one ordinary guidance request for a run at a time; release its gate on success, failure, or route change; discard stale responses; keep the student's unsent draft intact when blocked; compute controls from the active run's in-flight state.

**Ask First:** Changes to backend API behavior, model prompting, automatic guidance, or allowing parallel requests per run.

**Never:** Do not serialize deep diagnosis with ordinary guidance; do not cancel backend work or lose a student's draft as a side effect of the guard.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Duplicate keyboard submit | Same run already has a pending guidance request; draft is non-empty | No second API request; draft remains editable and intact | No duplicate user message |
| Route switches during request | Run A pending; run B becomes active | B controls are independent and can submit; late A result is discarded | A completion cannot unlock B while B is pending |
| Request settles | Current run request succeeds or fails | Only one response is rendered; the run gate releases | Error is shown and a later retry is allowed |

</frozen-after-approval>

## Code Map

- `app.js` -- `requestGuidance`, `sendChatMessage`, the Ctrl/Cmd+Enter listener, and run activation/clearing paths.
- `tests/student-guidance-concurrency.test.js` -- VM-level deferred-response tests for duplicate submits, per-run control state, stale results, and gate recovery.

## Tasks & Acceptance

**Execution:**
- [x] `app.js` -- gate ordinary guidance requests by run ID, retain drafts on rejected keyboard submits, and resync buttons whenever the active run changes -- prevent duplicates without cross-run UI races.
- [x] `tests/student-guidance-concurrency.test.js` -- exercise overlapping attempts, interleaved run A/B responses, failures, and retry -- lock the user-visible request contract.

**Acceptance Criteria:**
- Given a student has sent a guidance request for a run, when they press Ctrl/Cmd+Enter again before it settles, then no second request or duplicate user message is created and the draft remains in the composer.
- Given run A has a pending request, when run B becomes active, then B can submit independently and A's late result cannot appear in B's chat.
- Given requests for A and B are both pending, when A settles, then B's buttons remain disabled until B settles.
- Given a guidance request fails or succeeds, when it settles, then a new request for that same run can be submitted.
- Given a student uses the predefined “服软” action during the same run's pending request, when the request boundary is reached, then no parallel request is sent.

## Spec Change Log

## Design Notes

Track in-flight run IDs rather than one page-wide boolean. This allows navigation to another run without allowing two simultaneous requests for the same run; button state is derived from the active run, not whichever request finishes last.

## Verification

**Commands:**
- `node --test tests/student-guidance-concurrency.test.js` -- expected: all serialized-request and interleaved-response cases pass.
- `node --test tests/*.test.js` -- expected: complete frontend/API regression suite passes.
- `node --check app.js` -- expected: no JavaScript syntax errors.
- `git diff --check` -- expected: no whitespace errors.

**Implementation verification (2026-10-03):** Focused suite passes 5 tests and the complete frontend/API suite passes 40 tests. Both JavaScript syntax checks and `git diff --check` pass. Deferred promises verify duplicate keyboard submission, draft retention, separate run A/B gates, stale response suppression, failure recovery, and logout control reset. Independent blind, edge-case, and acceptance review found one logout-reset gap; it was fixed and covered by a regression test. The app/API are not running locally, so browser interaction has not been rechecked in a real session.

**Manual check:** In a live student browser session, hold a guidance response pending, press Ctrl/Cmd+Enter with a second draft, switch to another run, and log out. Verify one request per run, retained draft, no cross-run reply, controls following the active run, and recovery after logout. Not run: the local application/API are currently unavailable.
