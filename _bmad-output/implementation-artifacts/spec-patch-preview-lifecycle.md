---
title: 'Recover Patch Preview Actions After Replacement'
type: 'bugfix'
created: '2026-10-03'
status: 'in-review'
baseline_commit: 'c213781f4ddb5401cd303d78660591d60ef495c3'
context:
  - '{project-root}/app.js'
---

<frozen-after-approval reason="project-goal work order">

## Intent

**Problem:** Opening a second patch preview supersedes the first request by incrementing an epoch, but the previous patch's source button remains disabled after the active button reference is replaced. Students can lose access to an unapplied patch even though its preview was merely superseded.

**Approach:** When a new preview takes ownership of the dialog, restore the prior unapplied source button before replacing its reference. Preserve existing epoch checks so stale responses cannot replace the active preview; ensure failures and cancellation restore the active button.

## Boundaries & Constraints

**Always:** Keep one active preview dialog, restore every superseded unapplied button, preserve stale-response rejection, and leave successfully applied patches visibly unavailable for reapplication.

**Ask First:** Any change to patch application semantics, server APIs, hash validation, or confirmation policy.

**Never:** Do not apply a patch automatically, clear a student's workspace edits, or allow an older request to replace a newer preview.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Request replacement | Preview A pending; student opens preview B | A's unapplied button is restored; B owns the dialog | Late A success/failure is ignored |
| Active preview replacement | A preview is open; B is opened | A can be opened again later; B remains the active review | No stale diff replaces B |
| Failure or cancellation | Current preview fails or is closed | Current unapplied source button becomes enabled | Dialog clears or shows the current error |
| Applied patch | Student confirms the active patch | Applied patch button stays disabled with its applied label | Never re-enable an applied patch |

</frozen-after-approval>

## Code Map

- `app.js` -- `openPatchReview`, `closePatchReview`, patch epochs, and source-button ownership.
- `tests/patch-preview-lifecycle.test.js` -- deferred API-response tests for replacement, stale results, failure, cancellation, and applied-state preservation.

## Tasks & Acceptance

**Execution:**
- [x] `app.js` -- release a prior unapplied source button when a new preview supersedes it and guard preview ownership while apply is pending -- prevent orphaned controls and overlapping patch operations.
- [x] `tests/patch-preview-lifecycle.test.js` -- cover request replacement and all terminal paths -- prevent stale dialog and button state regressions.

**Acceptance Criteria:**
- Given preview A is pending, when the student opens preview B, then A's source button is enabled and B alone remains disabled for the active review.
- Given A resolves or fails after B has started, when the stale response arrives, then it cannot alter B's dialog, message, or button state.
- Given an active unapplied preview fails or is cancelled, when the dialog closes or reports failure, then its source button is enabled.
- Given the active patch is successfully applied, when the dialog closes, then its button remains disabled and marked applied.
- Given a patch is being applied, when another preview is requested, then the active operation is not superseded and the student receives a wait message.
- Given the student closed the dialog during application, when another preview is requested before the operation settles, then a visible dialog explains the wait and later reports the outcome.
- Given the student closes the preview while application is pending, when the server confirms success, then the source button is still marked applied and cannot be applied again.
- Given the route changes while application is pending and the operation fails, when the response arrives, then the stale dialog closes and the unapplied source button is restored.
- Given patch application succeeds but refreshing the editor file fails, when the request settles, then the patch remains marked applied and the refresh failure is not reported as an apply failure.

## Spec Change Log

## Design Notes

The source-button reference is an ownership token for the current dialog. Superseding the request must release the previous token before assigning the new one; incrementing the request epoch alone prevents stale rendering but does not restore UI state.

## Verification

**Commands:**
- `node --test tests/patch-preview-lifecycle.test.js` -- expected: all lifecycle and race cases pass.
- `node --test tests/*.test.js` -- expected: full frontend/API regression suite passes.
- `node --check app.js` -- expected: no JavaScript syntax errors.
- `git diff --check` -- expected: no whitespace errors.

**Implementation verification (2026-10-03):** Focused lifecycle suite passes 12 tests; complete frontend/API suite passes 52 tests. JavaScript syntax checks and `git diff --check` pass. Deferred-response tests confirm replacement, valid stale-success and stale-failure isolation, cancellation recovery, apply-lock recovery, visible success/failure after close, applied-state preservation across route-epoch changes, and distinction between patch-apply failure and editor-refresh failure. Blind, edge-case, and acceptance reviews completed; their actionable findings were fixed and rechecked. Real browser interaction remains unverified because the local app/API are not running.

**Manual check:** In a student browser session, open two diagnostic patch previews in quick succession; verify the earlier button becomes available again, the newer diff stays active when the old response returns, cancellation restores the current button, and an applied patch remains marked unavailable. Not run: no local app/API session is available.
