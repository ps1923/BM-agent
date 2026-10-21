---
title: 'DSH 插件基线整改'
type: 'feature'
created: '2026-10-21'
status: 'in-progress'
context: []
baseline_commit: 'c213781f4ddb5401cd303d78660591d60ef495c3'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** 当前 BM-HS 同时提供网页/Electron 编码、服务器工作区、代码指导和自动快照；教师证据、Bug 记忆授权及部署形态与“学生在集中 DSH 自建工作区，BM-HS 仅提供教学插件与治理”的基线冲突。

**Approach:** 将 DSH v0.2.1-rc.2 作为锁定兼容基线，在服务器为学生提供彼此隔离的 DSH 实例和持久工作区；BM-HS 以 Cordis 模型工具插件提供实验教学能力，继续承载课程、实验、教师和管理员流程。仅学生明确提交的 Bug 或所选代码快照进入审核/教师视图。

## Boundaries & Constraints

**Always:** 保留实验方向澄清、规划 Harness、分层树节点、MySQL 课程/实验/进度及教师评价。身份必须由已认证服务端凭据推导；服务端只保存 DSH 不透明标识，不接收绝对路径或信任工具传来的学生 ID。Bug 以 experimentId 为记忆池边界，数据库执行最终授权，只有学生手动提交且授权教师审核通过后可检索。代码仅按学生指定文件手动提交；提交快照不可变，教师不可访问实时工作区。个人实验审核教师须由管理员预绑定。10 名学生并发是验收目标。

**Ask First:** 生产部署、生产数据库迁移/切流、删除旧数据或提交推送 Git 均需另行批准。

**Never:** 不自动上传 DSH 对话、终端全文、实时文件、Bug 或快照；不自动完成学习节点、评分或修改学生代码；不让学生本机安装 Docker；不破坏性删除旧 Docker/Electron/历史记录。

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| 学生启动 DSH | 已登录学生 | 绑定个人隔离实例；重建后保留工作区 | 过期凭据拒绝；启动失败可重试且不串用户 |
| 手动提交 Bug | 当前实验运行与案例 | 待授权教师审核；通过后只入该实验记忆池 | 拒绝越权、跨实验或重复提交 |
| 手动提交代码 | 文件白名单及内容 | 保存不可变快照、哈希、节点关联并供授权教师查看 | 路径越界、文件/体积超限或越权时整体拒绝 |
| 旧运行记录 | 历史 Docker/Electron 数据 | 保持可读只读，不进入新 DSH 工作流 | 不兼容时显式说明，不覆盖迁移 |

</frozen-after-approval>

## Code Map

- `compose.yaml` — 当前仅编排两个规划 Harness 与 bm-api；无 DSH 实例服务。
- `backend/src/main/java/com/bmhs/workspace/` — 旧模板/Docker 工作区、编辑、终端和节点完成快照链路。
- `backend/src/main/java/com/bmhs/guidance/`、`diagnosis/`、`patch/` — 服务器代码读取、模型指导、诊断和补丁。
- `backend/src/main/java/com/bmhs/rag/` — Bug 提交、审批、MySQL 授权及 Chroma 检索。
- `backend/src/main/java/com/bmhs/teacher/`、`auth/`、`course/` — 教师证据、课程视图、登录和管理员扩展入口。
- `database/schema.sql`、`database/migrations/` — 增量 schema 与旧数据兼容。
- `app.js`、`index.html`、`styles.css`、`desktop/` — 当前 BM-HS 编辑器/终端/助手/Electron 学生入口。
- `README.md`、`compose.yaml` — 现有部署与学生运行说明。

## Tasks & Acceptance

**Execution:**
- [ ] `backend/src/main/java/com/bmhs/`、`database/` — 增量增加 DSH 实例/会话绑定、短期授权、个人实验审核教师、不可变代码提交及文件清单；服务端派生身份并限制配额，保留历史数据。
- [ ] `compose.yaml`、`backend/src/main/java/com/bmhs/` — 提供隔离 DSH 实例生命周期和无公网直连的反向路由；固定并验证 v0.2.1-rc.2，不把绝对路径传入 BM-HS。
- [ ] 新增 DSH Cordis 工具插件 — 注册实验列表/绑定/学习路径/进度、实验 Bug 记忆搜索与手动 Bug/代码提交；模型不得无确认静默提交。
- [ ] `backend/src/main/java/com/bmhs/rag/` — 以 experimentId 查询已审核案例，支持课程与个人实验，DB 授权为准；兼容旧 course_id 数据。
- [ ] `backend/src/main/java/com/bmhs/teacher/`、`auth/`、`app.js`、`index.html`、`styles.css` — 增加个人实验导师绑定与审核/提交视图，移除新运行对 BM-HS 编辑器、终端、指导、自动快照及聊天指标的依赖。
- [ ] `README.md`、`compose.yaml` 与相关测试 — 更新新旧流程和部署文档；旧 Docker/Electron 能力/数据只读兼容，不清理用户已有变更。
- [ ] 测试 — 覆盖插件真实 DSH 集成、身份/实验/教师隔离、重建与失败恢复、提交边界、RAG 回退及历史只读；对 10 名学生并发记录启动、P95、错误率、资源和恢复指标。

**Acceptance Criteria:**
- Given 教师已发布实验且学生已登录，when 学生打开 DSH，then 获得独立容器和持久空工作区，插件能绑定会话并读到实验节点，不泄露其他学生身份或工作区。
- Given 学生在某实验主动提交 Bug，when 授权教师通过审核，then 同实验其他运行可检索，未审核或其他实验案例不可泄漏。
- Given 学生选择工作区文件并明确提交，when 教师查看实验运行，then 仅提交文件快照及差异可见，实时工作区、DSH 对话和终端不可见。
- Given 学生更新节点/任务进度，when 插件记录事件，then 服务端验证授权并保留审计；进度不替代教师评分。
- Given 任一旧 Docker/Electron 运行记录，when 用户查看历史，then 原数据保持只读可读且迁移不覆盖、不删除。
- Given 10 名学生同时启动并使用插件，when 执行压力验收，then 输出可复核的延迟、错误率、资源和恢复数据；未达标项明确报告，不伪称通过。

## Spec Change Log

## Design Notes

DSH 实例只在服务器侧连接；浏览器不持有容器凭据。插件工具输入仅携带实验/节点/文件选择等业务参数，身份和所有权由绑定凭据推导。提交代码采用不可变版本，禁止以新快照覆盖旧证据。旧功能采用显式停用/只读迁移，不进行物理删除。

## Verification

**Commands:**
- `mvn -f backend/pom.xml test` — 后端单元与集成测试通过。
- `npm test` — 前端和 Electron 回归测试通过。
- `python -m pytest harnesses` — Harness 回归通过。
- DSH v0.2.1-rc.2 Web 实例集成与 10 并发实测 — 留存安装、工具注册、身份隔离、交互及压力结果。

**Manual checks:** 完整走通教师发布、学生登录并打开 DSH、自建工作区、插件读节点、主动进度更新、Bug 提交审核与同实验复用、手动文件提交、教师查看及评价；核验拒绝跨用户/跨实验访问，并检查旧页面明确只读/停用。
