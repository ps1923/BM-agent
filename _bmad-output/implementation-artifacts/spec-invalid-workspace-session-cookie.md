---
title: 'Reject Invalid Session Cookies on Workspace APIs'
type: 'chore'
created: '2026-10-03'
status: 'done'
baseline_commit: 'c213781f4ddb5401cd303d78660591d60ef495c3'
context:
  - '{project-root}/backend/src/main/java/com/bmhs/auth/SessionAuthenticationFilter.java'
  - '{project-root}/backend/src/test/java/com/bmhs/workspace/WorkspaceControllerWebTest.java'
---

<frozen-after-approval reason="continuation of project security work order">

## Intent

**Problem:** Workspace Web tests prove that a missing cookie is rejected, but do not prove that a supplied expired or forged session cookie is treated as unauthenticated.

**Approach:** Add an HTTP-level regression test for an unrecognized session token on a state-changing task endpoint. Prove the authentication filter passes the cookie token to the authentication service, then assert stable 401 behavior and zero repository interactions.

## Boundaries & Constraints

**Always:** Exercise the real session filter, controller, argument resolver, and exception handler; verify the invalid token was checked and no repository access occurred.

**Ask First:** Changes to cookie lifetime, authentication failure contract, or session revocation policy.

**Never:** Do not create or alter live sessions or access a production database.

</frozen-after-approval>

## Code Map

- `backend/src/main/java/com/bmhs/auth/SessionAuthenticationFilter.java` -- resolves cookie tokens into authenticated request context.
- `backend/src/test/java/com/bmhs/workspace/WorkspaceControllerWebTest.java` -- verifies workspace API authentication behavior.

## Tasks & Acceptance

**Execution:**
- [x] Send a workspace API request with an unrecognized session cookie; assert `401 AUTHENTICATION_REQUIRED` and zero repository interactions.

**Acceptance Criteria:**
- Given a request with an invalid session cookie, when it PATCHes protected task progress, then the authentication service receives the token and the request returns HTTP 401 with `AUTHENTICATION_REQUIRED`.
- Given that rejected request, when repository interactions are inspected, then the repository has zero interactions.

## Verification

- [x] Full backend run included `WorkspaceControllerWebTest`: 7/7 passed.
- [x] Full Spring Boot suite: 131/131 passed with cached Maven 3.9.9; opt-in MySQL test not run.
- [x] `git diff --check` passed.

## Suggested Review Order

- Check that the state-changing request uses an unrecognized cookie and requires the auth filter to validate that exact token.
  [`WorkspaceControllerWebTest.java:141`](../../backend/src/test/java/com/bmhs/workspace/WorkspaceControllerWebTest.java#L141)
- Confirm the filter only adds authenticated identity when the session token resolves to a user.
  [`SessionAuthenticationFilter.java:31`](../../backend/src/main/java/com/bmhs/auth/SessionAuthenticationFilter.java#L31)
