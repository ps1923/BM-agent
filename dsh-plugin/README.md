# BM-HS DSH model-tools plugin

Standalone Cordis bundle for DeepSeek Harness `dsh-v0.2.1-rc.2`. It adds model tools only: no sidebar, UI, editor, agent, terminal, or independent model calls. DSH remains responsible for coding and its workspace context; BM-HS provides experiment learning data, per-experiment approved Bug memory, and explicit student submissions.

## Runtime contract

The DSH instance launcher must inject these variables into each isolated student's DSH process/container:

- `BMHS_API_BASE_URL`: BM-HS origin. HTTPS is required except loopback development.
- `BMHS_DSH_CREDENTIAL`: 15-minute bearer credential minted by the server for that student and DSH instance.
- `BMHS_DSH_REFRESH_CREDENTIAL`: rotating, instance-bound refresh credential. The plugin exchanges it only with `/api/plugin/auth/refresh`, keeps both credentials inside its closure, and removes them from the process environment so child command tools do not inherit them. Refresh credentials are revoked when the instance stops; never persist or log either credential.
- `BMHS_DSH_WORKSPACE_ID`: random opaque workspace binding identifier, not a filesystem path. It must be unique to the DSH instance/workspace and must not encode a username or path.

The active DSH session id is read from the immutable tool execution identity (`exec.agent.id`). A run binding is kept per in-memory DSH session; after restart the student reattaches. BM-HS remains the authorization authority and must validate credential expiry, student ownership, experiment access, teacher authorization, payload limits, and run/node/task relationships on every call.

## Tools

| Tool | Behavior |
| --- | --- |
| `bmhs_list_experiments` | List experiments accessible to the credential's student. |
| `bmhs_attach_experiment_run` | Bind an experiment to the current DSH session and opaque workspace id. |
| `bmhs_get_learning_path` | Read the attached experiment's tree and task criteria. |
| `bmhs_update_node_progress`, `bmhs_update_task_progress` | Ask for one-time student approval, then record a supported student-confirmed progress status. |
| `bmhs_search_bug_memory` | Search the attached experiment's approved Bug pool only. |
| `bmhs_submit_bug_case` | Explicit Bug submission for teacher review; never publishes directly. |
| `bmhs_submit_code_snapshot` | Explicit immutable snapshot of selected workspace-relative regular text files. |

All mutations request a DSH one-shot `ctx.approval` decision and proceed only on `allowed-once`; missing approval service, unavailable channel, rejection, and cancellation fail closed. Thus installing this bundle in interactive DSH requires the standard DSH user-approval service and a human answerer. In unattended/headless mode these writes are intentionally unavailable.

## Server API contract expected by this plugin

The BM-HS backend provides these endpoints:

```text
GET   /api/plugin/experiments
POST  /api/plugin/runs
GET   /api/plugin/runs/{runId}/learning-path
PATCH /api/plugin/runs/{runId}/nodes/{nodeId}
PATCH /api/plugin/runs/{runId}/tasks/{taskId}
POST  /api/plugin/experiments/{experimentId}/bug-memory/search
POST  /api/plugin/runs/{runId}/bugs
POST  /api/plugin/runs/{runId}/code-submissions
```

`POST /api/plugin/runs` receives `{ experimentId, sessionRef, workspaceRef }` and returns a `runId`. The session reference is a hash of the current opaque DSH session identity; the workspace reference is an opaque per-instance id. Identity is derived exclusively from the bearer credential. Node/task progress bodies contain `{ status, studentConfirmed: true }`, and the backend validates authorization and state transitions. Bug submission uses `{ title, problem, solution }` and is pending teacher review. Code submission body contains `{ nodeId?, files: [{ path, content, sha256 }] }`; the backend independently validates path containment, content hashes, sensitive-file/content checks, limits, experiment ownership, and authorization. Only approved Bug cases may be returned from experiment-scoped search. Never expose live workspace files, DSH chat history, or terminal output through these endpoints.

## Install/use as a bundle

From a DSH 0.2.1-rc.2 profile with the required runtime already provisioned:

```powershell
dsh plugin --profile <profile> add <path-to-dsh-plugin>
dsh --profile <profile> --dump-config
```

Confirm `bmhs-learning-tools` appears once in the composed plugin entries. The exact runtime pin is intentional because DSH is pre-release and plugin APIs may change. Do not install this bundle into a profile that shares one credential/workspace among multiple students.

See [INTEGRATION-TEST.md](./INTEGRATION-TEST.md) for the real DSH smoke test. Unit tests run with `npm test` from this directory.
