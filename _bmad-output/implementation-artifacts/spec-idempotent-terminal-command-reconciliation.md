---
title: 'Reconcile Terminal Commands Without Accidental Re-execution'
type: 'feature'
created: '2026-10-03'
status: 'in-review'
baseline_commit: 'c213781f4ddb5401cd303d78660591d60ef495c3'
context:
  - '{project-root}/database/schema.sql'
  - '{project-root}/database/verify.sql'
  - '{project-root}/backend/src/main/java/com/bmhs/workspace/TerminalService.java'
  - '{project-root}/backend/src/main/java/com/bmhs/workspace/TerminalRepository.java'
  - '{project-root}/backend/src/main/java/com/bmhs/workspace/TerminalController.java'
  - '{project-root}/app.js'
---

<frozen-after-approval reason="user-authorized continuous project improvement">

## Intent

**Problem:** A terminal POST can execute a shell command while its response is lost or times out. The UI warns that the outcome is unknown but then unlocks the terminal, so a student can accidentally repeat a command with side effects.

**Approach:** Give each logical command a client-generated idempotency key, durably claim it before execution, and expose an owner-checked status lookup. Repeated submissions with the same key must return the existing command state/result and never invoke the runtime a second time. On uncertain transport outcomes, the UI queries status only; it never resends the shell command.

## Boundaries & Constraints

**Always:** Preserve student/session/workspace authorization and the per-workspace mutation lock. A durable `queued` or `running` record must survive request timeout or process interruption. If execution status cannot be proven, report it as unresolved and prohibit automatic re-execution. Reuse of one key with different command text or working directory returns a stable conflict. Persist only a UUID key and the existing bounded command/result data; never put command text in URLs or logs.

**Ask First:** Changing the terminal API contract outside the current app, or relaxing the no-duplicate-execution guarantee.

**Never:** Automatically retry a shell POST after timeout/network loss; infer success from a timeout; delete command history; or claim exactly-once side effects across arbitrary host/container crashes.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|---------------|----------------------------|----------------|
| New command | New UUID key and valid request | Persist claim, execute once, return final state | Preserve existing command errors |
| Duplicate | Same key and identical payload | Return original command state/result; do not call runtime again | Pending state remains queryable |
| Key misuse | Existing key with changed command or directory | No execution; stable `IDEMPOTENCY_KEY_REUSED` conflict | Do not overwrite original record |
| Lost response | POST outcome unknown | UI performs owner-checked GET lookup using same key | Poll with bounded backoff, then expose safe status-only retry |
| Unresolved record | Durable `queued`/`running` after interruption | Keep command marked unresolved; never automatically rerun | Tell student to inspect workspace before starting a new command |
| Missing/invalid key | No UUID key or malformed key | Reject before workspace mutation or runtime access | Stable 400 validation response |

</frozen-after-approval>

## Code Map

- `database/schema.sql` and a new additive `database/migrations/` script -- add nullable idempotency identity for legacy rows and a unique `(terminal_session_id, client_command_id)` constraint.
- `database/verify.sql` and `tests/database-migrations.test.js` -- verify the exact additive schema and migration behavior.
- `backend/src/main/java/com/bmhs/workspace/TerminalModels.java`, `TerminalController.java`, `TerminalService.java`, and `TerminalRepository.java` -- validate the key, durably claim before runtime execution, enforce same-payload reuse, and provide owner-scoped state lookup.
- `app.js` and `styles.css` -- create one key per user submission, persist pending reconciliation identity per tab, query status without reposting, and keep terminal controls safe while unresolved.
- Backend and frontend terminal tests -- cover identity reuse, authorization, crashes/pending status, route races, status polling, and no duplicate runtime call.

## Tasks & Acceptance

**Execution:**
- [x] Add additive command idempotency column/index and update schema verification without altering existing command history.
- [x] Persist command claims before execution in an independent transaction while retaining workspace mutation serialization; reject key/payload mismatch and add owner-scoped status retrieval.
- [x] Send one UUID key per logical browser command; after uncertain POST outcomes, query only the status endpoint and keep unresolved commands visibly non-repeatable, including after page reload.
- [x] Add migration, repository/service/controller, and UI regression tests for the covered matrix cases. Live database constraint/race and browser environment acceptance remain outstanding.

**Acceptance Criteria:**
- Given one valid key and command, when the student submits it more than once after a lost response, then the runtime executes it at most once and every response refers to the same command record.
- Given a key already claimed for different content, when the student reuses it, then the API rejects the request without executing another command.
- Given the POST response times out, when the UI reconciles, then it issues only status GET requests and does not unlock/repost while the result remains unresolved.
- Given a process interruption after the durable claim, when the student revisits the run, then the existing status is shown and no automatic execution occurs.
- Given another student requests the command status, when authorization is checked, then the API reveals no command or output data.

## Verification

- [x] Run terminal-focused backend tests, full Spring Boot tests, frontend/API tests, migration-shape checks, JavaScript syntax checks, and `git diff --check`.
- [ ] Run the additive migration/verifier and exercise timeout/reconciliation in an isolated MySQL + Docker browser environment before marking the UI stage accepted.

## Suggested Review Order

**执行与持久化边界**

- 入口先校验权限、重复键并持久化认领，再调用终端运行时。
  [`TerminalService.java:62`](../../backend/src/main/java/com/bmhs/workspace/TerminalService.java#L62)

- 独立事务认领命令，并以条件更新确认进入运行态。
  [`TerminalRepository.java:83`](../../backend/src/main/java/com/bmhs/workspace/TerminalRepository.java#L83)

- 状态读取沿学生身份过滤，输出日志与容器工作区分离。
  [`TerminalRepository.java:131`](../../backend/src/main/java/com/bmhs/workspace/TerminalRepository.java#L131)

**持久化结构**

- 可空旧记录兼容与会话内唯一键支撑增量迁移。
  [`schema.sql:516`](../../database/schema.sql#L516)

- 单独迁移不回写旧命令历史，可重复执行。
  [`20261003_add_terminal_command_idempotency.sql:1`](../../database/migrations/20261003_add_terminal_command_idempotency.sql#L1)

- 部署核验检查新增列、索引与既有结构指纹。
  [`verify.sql:188`](../../database/verify.sql#L188)

**浏览器恢复与权限接口**

- 刷新恢复只保存标识；损坏状态会锁定而不误判为空。
  [`app.js:959`](../../app.js#L959)

- 不确定响应只查状态，HTTP 408 不清除待确认身份。
  [`app.js:983`](../../app.js#L983)

- REST 入口将幂等键与学生会话绑定。
  [`TerminalController.java:29`](../../backend/src/main/java/com/bmhs/workspace/TerminalController.java#L29)

**回归证据**

- Web 测试覆盖缺失键、教师越权及学生状态查询。
  [`TerminalControllerWebTest.java:63`](../../backend/src/test/java/com/bmhs/workspace/TerminalControllerWebTest.java#L63)

- 服务测试验证重复键不重放及中断时保持未知状态。
  [`TerminalServiceTest.java:162`](../../backend/src/test/java/com/bmhs/workspace/TerminalServiceTest.java#L162)

- UI 测试验证超时与刷新恢复只执行状态 GET。
  [`terminal-session-recovery.test.js:332`](../../tests/terminal-session-recovery.test.js#L332)
