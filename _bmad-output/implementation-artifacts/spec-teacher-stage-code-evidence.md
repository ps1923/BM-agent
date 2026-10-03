---
title: '教师查看学生阶段代码证据'
type: 'feature'
created: '2026-10-01'
status: 'in-review'
baseline_commit: '8454527b846d8add4f39178501c9fa81ac7f91a0'
context:
  - '{project-root}/_bmad-output/implementation-artifacts/spec-task-progress-loop.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-create-bm-sql-schema.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** 教师只能看进度和过程元数据，不能核查阶段代码。现有学生快照没有节点关联或教师读取接口，节点完成也不自动留档。

**Approach:** 学生完成节点且任务校验通过后，系统保存一份关联节点的只读代码快照；教师按课程/阶段/节点查看快照文件。教师不能修改或运行学生代码。

## Boundaries & Constraints

**Always:** 每次读取均校验教师、课程、成员、发布实验、运行和快照归属；快照必须关联同一运行中的节点；路径规范化并拒绝符号链接/越界；源码按纯文本展示且限制文件数和大小；完成失败不得留下可见半成品；旧节点无快照须明确显示，不伪造历史；数据库迁移只增不删。

**Ask First:** 生产迁移/部署、删除快照、跨课程共享、将代码交给模型分析、改变任务完成规则。

**Never:** 教师写入工作区或执行命令；只凭客户端 ID 授权；泄露主机路径/存储键；把 HTML 当代码渲染；把当前代码冒充旧节点历史。

## I/O & Edge-Case Matrix

| Scenario | Expected behavior | Error handling |
|---|---|---|
| 任务未完成 | 节点/子节点不变，不生成阶段快照 | 返回任务未完成冲突 |
| 节点完成 | 生成一份节点快照并与完成状态同成同败 | 复制/写库失败则回滚并清理临时文件，可重试 |
| 完成请求重试 | 复用既有快照，不重复生成 | 旧节点无历史快照时显示缺失，不补造 |
| 教师查看 | 仅返回其有效课程运行的快照与文本文件 | 其他课程/失效成员拒绝访问 |
| 异常文件 | 不读取 `..`、绝对路径、链接、超大或非文本内容 | 400/413/415，不泄露本机路径 |

</frozen-after-approval>

## Code Map

- `WorkspaceService.java`, `WorkspaceRepository.java`, `WorkspaceRuntime.java`, `DockerWorkspaceRuntime.java` -- 快照文件、工作区进程暂停/恢复与运行/节点进度事务。
- `TeacherRepository.java`, `TeacherService.java`, `TeacherDashboardController.java`, `TeacherModels.java` -- 课程授权与教师读取 API。
- `database/schema.sql`, `database/migrations/`, `database/verify.sql` -- 节点关联、幂等索引、增量验证。
- `app.js`, `index.html`, `styles.css` -- 教师阶段快照列表及只读文件预览。
- `backend/src/test/java/com/bmhs/{workspace,teacher}/`, `tests/` -- 权限、事务、路径和页面回归。

## Tasks & Acceptance

**Execution:**
- [x] `database/schema.sql`, `database/migrations/`, `database/verify.sql` -- 增加可空 `node_id` 和阶段快照幂等约束/只增迁移 -- 保留旧快照。
- [x] `WorkspaceService.java`, `WorkspaceRepository.java` -- 任务检查后生成临时快照、节点关联、事务提交后可见，回滚时清理 -- 保持节点/快照一致且重试幂等。
- [x] `TeacherRepository.java`, `TeacherEvidenceService.java`, `TeacherEvidenceController.java`, `TeacherModels.java` -- 增加逐请求授权的阶段、文件清单、文件读取 -- 只开放本课程运行。
- [x] `app.js`, `index.html`, `styles.css` -- 按阶段/节点呈现快照、文件树、纯文本和缺失/空状态 -- 不伪装历史成果。
- [x] `backend/src/test/java/com/bmhs/{workspace,teacher}/`, `tests/` -- 增加节点快照、教师授权边界、路径拒绝和 UI 合约回归测试 -- 防泄露及虚假证据。

**Acceptance Criteria:**
- Given 节点有未完成任务，when 学生直接完成节点，then 进度不变且没有节点快照。
- Given 任务全部完成，when 学生完成节点，then 节点关联唯一快照；快照失败则节点不完成、子节点不解锁。
- Given 教师属于该有效课程，when 查看阶段快照，then 仅能读到该运行下、根目录内的受限文本文件。
- Given 其他教师或失效成员请求，when 读取快照，then 拒绝且不泄露存在性/内容。
- Given 历史节点无快照，when 教师查看，then 明确显示无历史记录，不显示当前代码冒充历史。

## Design Notes

文件系统与 MySQL 不共享事务：先写不可见临时目录，任务/节点锁校验后原子改名并写元数据；事务回滚回调清理目录。唯一约束防并发重复。节点证据写在工作区同级的 `.node-evidence` 目录，不挂载到学生容器，避免学生改写历史快照；新的手动/补丁前快照写在工作区同级的 `.manual-snapshots`，不暴露给学生容器。快照从实际工作区采集 Java/Python 源码、测试及文档 UTF-8 文本，而非只复制文件索引；机器配置文件默认不进入不可变教师快照，避免 XML/YAML/Properties 等不同语法下凭据脱敏遗漏。常见文本凭据模式再做脱敏，但不保证识别任意自定义秘密。跳过 `.git`、`.snapshots`、`.terminal` 和构建/依赖产物，并限制目录项、文件数、单文件及总大小。学生文件 API 拒绝快照保留路径。生产默认要求 `SecureDirectoryStream` 与 `NOFOLLOW_LINKS` 锚定目录句柄，按文件身份和重复读取校验稳定性，回滚也通过安全目录句柄恢复；Linux 生产文件系统支持性待实测。仅测试构造使用路径复核的回退读取。终端命令、文件写入、补丁应用和快照由工作区数据库行锁协调；对写入、补丁、手动快照及节点证据操作，Docker 工作区在文件访问期间保持暂停，并在数据库事务回滚回调完成后恢复，以防容器内遗留进程在哈希校验与原子替换间改写目标文件；暂停失败时拒绝操作。只读索引不暂停容器，依赖稳定读取与响应 SHA-256 校验拒绝不一致数据，避免打断学生正在运行的实验。文件替换和回滚保留 POSIX 权限。该暂停保护不覆盖宿主机上绕过应用和容器的特权进程，仍需保持工作区存储目录的最小宿主机写权限。工作区启动状态原子转换 `provisioning -> starting`，防止并发请求启动同一运行容器。节点外键采用 RESTRICT 以保留已有阶段证据；教师每次以快照反查课程授权，浏览器不接收服务器路径，源码用 `textContent` 展示。旧版 `.snapshots` 路径下已有文件未做破坏性迁移，仍需在升级前审查其兼容访问策略。

## Verification

**Commands:**
- `mvn -f backend/pom.xml test` -- 完整后端单元/Mock 测试 124 项通过、0 失败。Maven 3.9.10 仅临时下载并验证 SHA-512 后运行，未全局安装。回归涵盖容器内文件操作前暂停/事务后恢复、暂停失败时不改文件、多文件补丁共用单次暂停、回滚期间保持隔离，以及终端生成文件索引、哈希刷新、敏感/构建文件过滤和总字节预算上限；其余覆盖终端/Docker CLI 并发排空和输出限额、UTF-8 截断、Docker 容器 ID 严格格式、节点路径 NUL 拒绝、快照保留目录拒写、机器配置排除、凭据脱敏、文件权限保留、并发变更回滚、运行失败清理，以及深度诊断信任边界/字符预算/严格路径引用/工作区变更检查。Windows 测试构造使用兼容回退文件操作，生产 SecureDirectoryStream 分支和 Linux 文件系统支持情况尚未实际验证。这不是 MySQL 集成测试，不能证明数据库事务回滚、启动状态原子认领或迁移已在目标数据库成功。
- `node --check app.js`; `node --test tests/*.test.js` -- 前端脚本检查通过，17 tests passed；覆盖 API 配置、迁移合同、教师证据文本呈现、文件路径/工作区归属/SHA-256 内容一致性校验、文件标签/哈希保存、终端前未保存代码保护、终端后工作区文件安全重同步（含实际运行同步函数与过期运行响应丢弃）及教师/任务请求过期保护。
- `git diff --check` -- 通过；Git 仅提示工作区文件有 LF/CRLF 规范化提示。
- `database/verify.sql` -- 已编写但尚未在获批测试库运行；增量迁移和旧快照保留情况待真实数据库验收。

**Manual checks:** 真实学生完成节点、重复提交、刷新；教师看同课代码；用异课教师/失效成员及越界、链接、超大、二进制路径验证拒绝；注入快照失败确认回滚；既有手动/补丁快照仍可用。

**Current verification result (2026-10-01):** Backend full suite 124 passed, 0 failed; Maven 3.9.10 was run from a temporary directory after SHA-512 verification. Frontend syntax check and 17 tests passed; project `.venv` Python suite: 38 passed (17 Harness/provider tests and 21 RAG benchmark-evaluator tests); deterministic live MCP smoke test: both services exposed exactly one expected tool and the planner returned a four-node tree. Terminal and Docker CLI process output are drained concurrently with bounded byte retention; the service also independently enforces the terminal output cap and marks truncation without splitting UTF-8 characters. Docker CLI startup output accepts only a valid container ID. The workspace editor verifies file path, workspace identity and response SHA-256 before display/save. Terminal-created and modified supported text files are now re-indexed on listing under per-workspace bounds, with generated/dependency outputs and sensitive filenames excluded; terminal actions are blocked while editor content is unsaved, and the UI re-fetches/re-verifies the selected file after command completion. A runtime test executes the actual synchronization helper and confirms the disk version/hash reach the editor while a response from a switched run is discarded. Before workspace writes, patches and snapshots, the running Docker workspace is paused under the workspace transaction lock and resumed after transaction completion, including rollback callbacks; failure to pause rejects mutation. Reads stay non-pausing and fail closed on path/hash mismatch. Unit tests cover pause command allowlisting, no-write-on-pause-failure, multi-file patch single-pause behavior, and restoration-before-resume on rollback. This is not yet a real Docker timing test. The local Node mock at port 8080 returns `src/main.py` and Python `print("route ok")` content, but its detail response omits required workspace identity/hash and its list hash is the placeholder `test`; the updated frontend correctly rejects this incomplete metadata. Port 8080 is not Spring Boot, so this does not prove real API behavior. The original user browser tab was left untouched to avoid losing unsaved work. The local `GET /api/runs/42/progress` response also omits the required `tasks` array; the page visibly shows “读取失败” and a retry control, disables task completion and node completion, and does not claim progress was saved. Teacher-authenticated flows, node completion, RAG, patches and the complete end-to-end workflow remain unverified. Docker CLI checks are unit-level only; Docker Engine is unavailable, so real container startup/cleanup and terminal execution are not verified. `pytest.ini` now discovers both Python test areas by default. The temporary Harness services were stopped after the smoke test. `git diff --check` passed with existing LF/CRLF normalization notices. A local `mysqld.exe` is listening on 3306, but no credentials were used and no database was read or changed. Migrations, MySQL transaction rollback, production Linux `SecureDirectoryStream` support, real teacher isolation and complete student-to-teacher workflow remain unverified. A process interruption in `starting` state is deliberately not auto-recovered: a safe retry requires fencing against a slow prior Docker start, which the current schema lacks. The Windows test path uses an explicit non-production compatibility mode. Snapshot and deep-diagnosis code redact common credential-shaped values, but detection cannot guarantee removal of arbitrary secrets; production acceptance should include secret-scanner validation or a stricter reviewed-file allowlist. Container mutation by host-level processes that bypass the app/container remains outside this pause guard. This feature remains `in-review`.
