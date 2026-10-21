---
title: 'Keep Teacher Course Dashboard Usable Without Local Storage'
type: 'bugfix'
created: '2026-10-03'
status: 'in-review'
baseline_commit: 'c213781f4ddb5401cd303d78660591d60ef495c3'
context:
  - '{project-root}/app.js'
---

<frozen-after-approval reason="project-goal work order">

## Intent

**Problem:** The teacher dashboard writes its course selection defensively when storage is unavailable, but reads the saved selection without a guard. In restricted browser contexts, that exception falls into the dashboard's general error path and can hide otherwise available courses and student progress.

**Approach:** Treat the saved course as an optional preference: tolerate storage read/write failures and fall back to a valid owned course, while keeping course selection and dashboard requests fully functional.

## Boundaries & Constraints

**Always:** Course data and switching must remain usable if browser storage access throws; only restore a saved course if it still belongs to the teacher; use the first owned course as fallback.

**Ask First:** Changes to course authorization, server APIs, or course selection policy.

**Never:** Do not make local storage a prerequisite for loading teacher courses or progress.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Storage read denied | Owned courses load; `getItem` throws | First owned course is selected and its dashboard loads | No page-level failure caused by storage |
| Storage write denied | Teacher selects a different owned course; `setItem` throws | Selected course dashboard still loads and updates | Ignore persistence failure |
| Stale saved course | Saved ID is absent from owned courses | First owned course is selected | Do not request an unauthorized/stale ID |

</frozen-after-approval>

## Code Map

- `app.js` -- `renderTeacherDashboard` saved-course restoration and `loadTeacherCourseDashboard` persistence.
- `tests/teacher-dashboard-storage.test.js` -- execute production dashboard functions with denied storage and deferred course responses.

## Tasks & Acceptance

**Execution:**
- [x] `app.js` -- make course preference reads optional and retain the existing best-effort write behavior -- prevent browser storage policy from breaking course management.
- [x] `tests/teacher-dashboard-storage.test.js` -- verify dashboard load and course switching with read/write failures and stale saved IDs -- protect the real route behavior.

**Acceptance Criteria:**
- Given the teacher's storage read throws and courses are available, when the teacher dashboard renders, then the owned course list and first course progress load without a storage error.
- Given the teacher selects another owned course while storage writes throw, when the change is processed, then the selected course dashboard still loads.
- Given storage contains an ID not owned by the teacher, when the dashboard restores selection, then it falls back to the first owned course.

## Spec Change Log

## Design Notes

Course preference persistence is an enhancement, not part of course authorization or dashboard availability. The server remains authoritative for the courses the teacher owns.

## Verification

**Commands:**
- `node --test tests/teacher-dashboard-storage.test.js` -- passed: 3/3 cases.
- `node --test tests/*.test.js` -- passed: 55/55 frontend/API tests.
- `node --check app.js` and `node --check tests/teacher-dashboard-storage.test.js` -- passed.
- `git diff --check` -- passed (only Git line-ending normalization warnings for existing CRLF/LF-mixed working files).

**Manual check:** Not completed. The local app at `http://127.0.0.1:8000/#/teacher` refused the browser connection, so actual-page interaction with storage blocked still requires a running frontend and backend.
