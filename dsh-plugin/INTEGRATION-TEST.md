# DSH v0.2.1-rc.2 integration test

This is an operator-run test against a disposable Web profile and a staging BM-HS API. It is not a production deployment instruction. Use two test students and two isolated DSH workspaces. Never use production credentials or student source code.

## Setup

1. Provision a disposable DSH `0.2.1-rc.2` Web profile with the standard workspace filesystem (`ctx.fs`) and interactive user-approval channel enabled.
2. Configure the BM-HS plugin API base URL, a 15-minute access credential, a rotating instance-bound refresh credential, and a random opaque workspace id unique to test student A. Do the same with different values for student B.
3. Install the local bundle using `dsh plugin --profile <test-profile> add <absolute-path-to-this-directory>`. Inspect `dsh --profile <test-profile> --dump-config`; the plugin row must appear exactly once. Restart the profile after environment configuration changes.
4. Add a staging experiment available to A and B and at least one approved Bug case in that experiment. Add a separate experiment with a distinct Bug case.
5. In A's workspace create `src/hello.py` with harmless text. Also create a disallowed `.env` sentinel file; do not put a real secret in it.

## Checks

1. Call `bmhs_list_experiments`; confirm the API receives only the short-lived Authorization credential and no student id field.
2. Attach the experiment. Confirm DSH prompts for one-time approval, rejection produces no run, and approval creates one run associated with A's session and opaque workspace id. Inspect API logs: no absolute path and no student id from tool arguments.
3. Read the learning path, then update a node/task. Reject one approval and approve another; only the approved update should reach the API.
4. Search Bug memory. Confirm the approved case from the attached experiment is returned and the case from the other experiment is absent.
5. Submit a Bug case. Confirm one-time approval is required, the saved case is pending review, and it is not returned by search until teacher approval.
6. Submit only `src/hello.py` as a code snapshot. Confirm the approval card names the exact selected file; the API receives its relative path, content, and SHA-256, with no DSH chat or terminal data. Confirm teacher views are backed only by this snapshot.
7. Attempt `.env`, `../outside.txt`, `C:\outside.txt`, an absolute POSIX path, a symlink resolving outside the workspace, a private-key marker, and a file over the size limits. Each must be rejected before the snapshot API call. Verify the containment check uses `ctx.fs.contains()` after resolving from `ctx.fs`.
8. Repeat A's run id or experiment id using B's credential directly against staging endpoints. The backend must reject it; plugin-side validation is not the authorization boundary.
9. Expire A's access credential and repeat a read and a write. Each must perform one refresh exchange, rotate the refresh credential, and retry once without logging either value. Revoke A's refresh credential and repeat: the request must fail closed. Remove the approval provider and repeat a write: it must fail closed without an API call.
10. Unload the plugin or close the test profile. Confirm Cordis disposes the registrations. Reload and confirm every tool appears exactly once.

## Expected evidence

Record the exact DSH version, profile, tool schema list, sanitized request method/path/body field names, approval outcomes, backend responses, and pass/fail for each check. Redact bearer values, source contents, and any personal data. A passing isolated unit test is not evidence that this integration test has passed.
