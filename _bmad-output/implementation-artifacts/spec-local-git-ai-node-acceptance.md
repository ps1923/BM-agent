---
title: '本地编码、Git 提交与 AI 节点验收'
type: 'feature'
created: '2026-10-22'
status: 'in-review'
baseline_commit: '9943bed3de5fc0d904a51af4c16c6561f2f9e54f'
context:
  - '{project-root}/database/schema.sql'
  - '{project-root}/README.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** 学生编码仍被引导到 DSH 或 BM-HS 网页/服务器工作区，现有 DSH 快照并非 Git commit，也不会触发 AI 节点验收；节点在前后端均被锁定。

**Approach:** 学生在本地 IDE 编码，手动 push 至 Gitea；系统按学生选定的一个或多个节点对不可变 commit 做 AI 验收，记录结果与证据。学生确认 Bug 案例、教师审核后才加入实验级 RAG。

## Boundaries & Constraints

**Always:** 节点是提示和检查点，可并行或一次检查多个；验收结果只更新建议状态，不限制后续学习、不自动评分；每个 commit 和验收结果留历史；只读取已 push 的 commit；Python、Java、前端均可静态检查；有明确运行命令时才尝试隔离、限时的可选运行；按学生、实验和教师权限隔离；保留既有 DSH、工作区、快照和进度数据。

**Ask First:** 生产服务器部署、生产数据库迁移/切流量、删除历史服务或数据。

**Never:** 新流程依赖 DSH 编辑器或 BM-HS 在线代码编辑；读取未提交的本地文件、自动采集 DSH 对话/终端；AI 自动评分或阻止学生继续；未审核 Bug 进入共享检索；运行验收代码时开放不受限网络或宿主机权限。

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| 多节点提交 | 学生 push commit 并选多个节点/全部 | 同一 SHA 对各节点独立生成有证据的验收结果 | 无效或跨实验节点拒绝登记 |
| 重复 webhook | 同仓库、同 SHA 重复事件 | 不重复建任务或调用模型 | 幂等返回既有任务 |
| 新版本提交 | 同仓库更高/不同 SHA | 新建验收历史，不覆盖旧结果 | 分析失败保留失败状态及可重试入口 |
| 可选运行检查 | 明确且允许的运行命令 | 受限容器中限时运行，结果与静态结论分开 | 环境不支持、超时或不安全时标记未验证 |
| Bug 复用 | 学生确认后提交案例，教师审核 | 通过后仅进入对应实验记忆池 | 待审/拒绝案例不可被其他学生检索 |

</frozen-after-approval>

## Code Map

- `app.js`, `index.html`, `styles.css` -- 学生节点入口、运行页、教师 DSH/快照视图。
- `backend/src/main/java/com/bmhs/dsh` -- 当前插件提交、运行绑定和节点锁定逻辑。
- `backend/src/main/java/com/bmhs/diagnosis` and `guidance` -- 可复用模型/结构化诊断组件；当前诊断绑定实时工作区，不能直接用于 commit。
- `backend/src/main/java/com/bmhs/rag` -- Bug 提交、教师审核和实验范围 RAG。
- `backend/src/main/java/com/bmhs/teacher` -- 教师进度、证据和评价。
- `database/schema.sql`, `database/migrations`, `compose.yaml` -- 现有运行数据、增量迁移与服务编排。
- `tests/`, `backend/src/test/java` -- UI 静态/交互与服务、迁移回归测试。

## Tasks & Acceptance

**Execution:**
- [x] 增加 Gitea 配置和每个学生实验运行实例的仓库映射、凭据保护及签名 webhook；按仓库/SHA 去重。
- [x] 增加 push commit 登记、节点多选关联、异步 AI 验收任务及逐节点不可变报告；校验 commit 属于该运行实例仓库。
- [x] 增加专用验收 Harness：只取固定 SHA 文件、节点标准和审核通过的实验 Bug；结构化输出结论、路径/行证据、建议；本轮只启用静态检查，可选受限执行暂缓。
- [x] 移除新流程对 DSH/网页/服务器编辑器的依赖；提供仓库说明、提交选择、验收状态及历史入口；旧路由/数据只读兼容。
- [x] 教师页面展示 commit、差异、逐节点验收历史与教师评价；保留 Bug 案例确认及教师审核入口。
- [x] 取消新流程的节点锁定及父子解锁；增量迁移脚本保留旧数据。
- [x] 更新部署与使用文档；新增 Gitea、webhook、权限、幂等及失败恢复说明和自动化回归测试。
- [ ] 在隔离 MySQL/Gitea 环境完成迁移、真实 push/webhook/模型报告/教师查看页面端到端验收；不得以 mock 测试替代。

**Acceptance Criteria:**
- Given 学生有授权实验，when 在本地 push 并登记一个、多项或全部节点，then 每项按该 commit 的内容独立验收并可查看证据和建议。
- Given 学生选取非根节点，when 浏览或提交该节点，then 不因父节点状态被阻挡。
- Given webhook 重发或新 commit 到达，when 系统处理，then 同 SHA 幂等、不同 SHA 保留独立历史。
- Given 报告有验收问题，when 学生确认 Bug 草案且教师批准，then 仅当前实验可检索该案例。
- Given 教师有授权，when 查看学生运行，then 仅可访问该实验已 push 的提交、差异、报告和进度。
- Given 旧运行已有工作区或快照，when 升级应用，then 旧数据仍可读取且不被新流程覆盖。

## Design Notes

`code_submissions` 当前保存文件全文且最多关联一个节点；不可变 Git 提交和节点验收历史应独立建模，避免把旧快照误当 commit。Gitea webhook 是 push 通知，提交与所选节点的登记必须通过已认证学生 API 绑定；不得相信 webhook 请求携带的学生身份。每个运行实例首次登记某 SHA 时固定关联节点集合；重复登记仅在 SHA 与节点集合均相同时幂等返回，不支持同一 SHA 后续补选/替换节点；需检查其他节点时，创建新的 commit 再登记。

## Verification

**Commands:**
- `cd backend && mvn test` -- 所有后端单元/集成测试通过。
- `node --test tests/*.test.js` -- 前端/API 静态与页面行为测试通过。
- `python -m pytest harnesses` -- Harness 测试通过。
- 用隔离 Gitea 测试实例验证本地 commit/push → 签名 webhook → AI 逐节点报告 → 教师查看全链路。

**Manual checks:** 实际走查学生仓库提交与多节点选择、报告刷新/历史、教师差异查看、Bug 确认审核；确认新页面不再打开 DSH 编辑器，任意节点可进入；检查三种技术栈和运行失败时的“无法验证”提示。

## Review Notes

- 独立盲审、边界审查和验收审计发现的问题已修复：webhook 请求体读取上限、行级差异统计/截断标记、失败任务恢复与重试、无证据的符合结论降级、超出上下文即拒绝、Git 课程看板进度、空节点实验拒绝创建、并发重复 SHA 登记幂等、Git 自我确认进度验证语义。
- 相同 SHA 的节点集合固定是本轮明确约定，避免同一不可变提交的验收范围被悄悄改写。
- 本地自动化：Spring Boot 205 项、Node 107 项、Python Harness 85 项测试通过；Docker Compose 配置检查通过。未执行数据库迁移、真实 Gitea push/webhook、模型调用或登录后浏览器端到端验收；生产部署未执行。

## Suggested Review Order

**Git 提交与验收入口**

- 先看 API 和身份边界，再沿提交状态追踪到后台任务。
  [`GitAcceptanceController.java:20`](../../backend/src/main/java/com/bmhs/git/GitAcceptanceController.java#L20)

- 固定仓库 SHA、排队、授权、Bug 确认和失败恢复的编排集中于服务层。
  [`GitAcceptanceService.java:112`](../../backend/src/main/java/com/bmhs/git/GitAcceptanceService.java#L112)

- 模型只接收固定 commit 与审过的实验记忆，并校验代码证据。
  [`GitCodeAcceptanceHarness.java:25`](../../backend/src/main/java/com/bmhs/git/GitCodeAcceptanceHarness.java#L25)

**数据与权限**

- 查看实验运行和节点关联如何幂等登记，以及教师读取如何按授权过滤。
  [`GitSubmissionRepository.java:31`](../../backend/src/main/java/com/bmhs/git/GitSubmissionRepository.java#L31)

- 增量表保留旧运行数据，验收结果按提交和节点独立保存。
  [`20261022_add_git_commit_acceptance.sql:1`](../../database/migrations/20261022_add_git_commit_acceptance.sql#L1)

- 新 Git 进度允许任意节点更新，旧工作区写入口显式停用。
  [`WorkspaceRepository.java:247`](../../backend/src/main/java/com/bmhs/workspace/WorkspaceRepository.java#L247)

**学生与教师体验**

- 查看学生从实验入口进入本地 Git 提交与节点选择的页面逻辑。
  [`app.js:2395`](../../app.js#L2395)

- 查看教师如何呈现已提交代码差异和节点验收报告。
  [`app.js:795`](../../app.js#L795)

**验证与部署支撑**

- 自动化覆盖权限、证据、重试、迁移和界面契约；最后再检查部署参数。
  [`GitAcceptanceServiceTest.java:1`](../../backend/src/test/java/com/bmhs/git/GitAcceptanceServiceTest.java#L1)

- Gitea 的内部服务、密钥参数和容器边界在 Compose 中定义。
  [`compose.yaml:1`](../../compose.yaml#L1)
