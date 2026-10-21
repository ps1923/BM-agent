---
title: 'Teacher Run Detail Partial-Failure Resilience'
type: 'bugfix'
created: '2026-10-03'
status: 'in-review'
baseline_commit: 'c213781f4ddb5401cd303d78660591d60ef495c3'
context:
  - '{project-root}/index.html'
  - '{project-root}/tests/teacher-evidence-ui.test.js'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The teacher run detail loads the summary and timeline with `Promise.all`. If either endpoint fails, the shared catch clears both sections, hiding valid information and making a transient timeline problem look like the entire run is unavailable.

**Approach:** Load and render summary and timeline independently. Preserve successful data, display a section-specific retry action for the failed request, and discard late results when the teacher changes runs.

## Boundaries & Constraints

**Always:** Keep existing teacher authorization, data contracts, and current-run guards; report unavailable data honestly; retries target only the failed section; keep summary, review form, AI summary, metrics, and timeline independently recoverable.

**Ask First:** Any backend/API, authorization, schema, or teacher evaluation policy change.

**Never:** Do not synthesize zero-valued metrics after a failed summary; do not clear successful summary data because timeline loading failed; do not render a response for a previously selected run.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Timeline unavailable | Summary succeeds; timeline rejects | Summary and review remain visible | Timeline shows scoped error and retry |
| Summary unavailable | Timeline succeeds; summary rejects | Timeline remains visible | Summary shows scoped error and retry |
| Both unavailable | Both requests reject | Each section independently explains its failure | Either section can be retried without clearing the other |
| Run changes | Old request resolves after another run is selected | Old data is discarded | Current run remains unchanged |

</frozen-after-approval>

## Code Map

- `app.js` -- `loadTeacherRunDetail`, run-detail epochs, reset, summary rendering, timeline rendering, and teacher review submission state.
- `index.html` -- `teacherRunSummary`, `teacherRunMetrics`, `teacherTimeline`, and review form inside the selected-run detail.
- `styles.css` -- shared `.form-message` and `.text-action` patterns for inline status and retry controls.
- `tests/teacher-evidence-ui.test.js` -- Node VM harness for route-scoped asynchronous teacher UI behavior.

## Tasks & Acceptance

**Execution:**
- [x] `index.html` -- add independent polite status regions for summary and timeline -- make loading, failure, and retry state clear to screen readers.
- [x] `app.js` -- load and render run summary and timeline independently -- preserve whichever panel succeeds when its sibling request fails.
- [x] `app.js` -- add section-scoped retry and stale-run guards -- retry only failed data and discard results for a run no longer selected.
- [x] `app.js` -- keep review submission disabled until summary/review data loads -- prevent an unavailable summary from appearing as a new empty review.
- [x] `app.js` -- explain the review loading gate and recovery action inline -- prevent the disabled evaluation controls from appearing broken.
- [x] `tests/teacher-evidence-ui.test.js` -- cover both partial-failure directions, both failures, independent retries, review state, and out-of-order run changes -- lock the recovery contract.

**Acceptance Criteria:**
- Given a teacher opens a student's run, when summary and timeline requests start, then each panel shows its own loading state without blocking the other panel from rendering.
- Given the summary succeeds and timeline fails, when the timeline request rejects, then summary metrics and the existing teacher review remain visible and only the timeline panel shows its error and retry.
- Given timeline succeeds and summary fails, when the summary request rejects, then timeline events remain visible and only the summary panel shows its error and retry; the review form remains disabled until existing review data is loaded.
- Given summary is loading or unavailable, when the review form is disabled, then its live status explains that the saved review must load first and directs the teacher to retry the summary.
- Given both requests fail, when both settle, then each panel independently explains its failure and offers a retry for its own endpoint.
- Given a failed panel is retried, when its endpoint succeeds, then only that panel is replaced; sibling panel content and the selected run remain unchanged.
- Given a teacher changes the selected run while either initial or retry request is pending, when the old response succeeds or fails, then it cannot update the newly selected run or its status messages.
- Given any panel renders server text, when it is displayed, then it uses text content rather than HTML injection; retry controls have accessible names and are keyboard operable.

## Spec Change Log

## Verification

**Design Notes**

The summary response owns run status, metrics, and the saved teacher review; the timeline response owns only event rows. Keep the existing `teacherRunDetailRequestEpoch` and `selectedTeacherRunId` as the authority for both child requests, checking both after each await and before showing errors. A panel retry invalidates only that panel's prior request; it must not reset the run detail or clear its sibling. Do not show fabricated zero metrics when summary is unavailable. Keep the review form disabled until a valid summary response arrives. Stage-code evidence retains its existing independent loader and is outside these two request states.

**Commands:**
- `node --test tests/teacher-evidence-ui.test.js` -- expected: all teacher evidence and run-detail failure/retry cases pass.
- `node --test tests/*.test.js` -- expected: the full browser/API regression suite passes.
- `node --check app.js` -- expected: no JavaScript syntax errors.
- `git diff --check` -- expected: no whitespace errors.

**Implementation verification (2026-10-03):** The focused teacher-evidence UI suite passes 20 tests; the current complete frontend/API suite passes 82 tests. `node --check app.js` and `git diff --check` pass. The suite exercises the route-panel functions in a DOM test harness, including partial failures, retries, keyboard activation, review gating and its explanatory message, malformed/inconsistent summary rejection, invalid timeline entries, and stale responses after switching runs. The requested real-browser fault-injection checks have not yet been run because no current application page/API is listening on ports 8000/8080; keep acceptance at `in-review` until those visual checks are completed.

**Manual checks:**
- In a teacher browser session, fail only `/teacher/runs/{id}/timeline`; verify summary, metrics, and saved review remain visible, then retry and confirm only the timeline updates.
- Repeat with only `/teacher/runs/{id}/summary` failing; verify the timeline remains, no zero metrics are fabricated, the reason the review form is disabled is visible, and retry enables it only after the saved review loads.
- Fail both endpoints independently, restore each, and retry one at a time; confirm each retry preserves its sibling panel.
- Open run A, select run B before A resolves, then resolve/reject A; verify no A data or error appears in B's detail.
