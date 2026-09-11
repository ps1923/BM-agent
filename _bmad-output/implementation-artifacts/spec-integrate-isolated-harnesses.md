---
title: '接入两个隔离的实验创建 Harness'
type: 'feature'
created: '2026-09-11'
status: 'done'
baseline_commit: 'NO_VCS'
context:
  - '{project-root}/README.md'
  - '{project-root}/database/schema.sql'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** 项目只有静态前端和 MySQL 结构，创建实验仍由浏览器模拟，无法完成真实对话、模型规划和持久化。

**Approach:** 新建 Spring Boot 核心后端，通过 MCP Streamable HTTP 分别调用两个独立 Python Harness 服务：需求端只确认意图，规划端只接收确认意图并生成由根节点持续分叉的“实验树→节点→任务”；Java 后端校验后事务落库并计算树状画布布局。

## Boundaries & Constraints

**Always:** Harness 使用独立进程、提示词、工具白名单和会话空间；规划器只接收带版本与哈希的 `ConfirmedIntent`；需求端每轮最多一问且不能生成节点；计划必须是有根树，每个非根节点只有一个父节点，至少 3 层，下一层节点数必须多于上一层（至少形成 1→2→3），不能退化为单链；每节点有 3–7 个可执行任务；凭据走环境变量；确认前不创建实验。

**Ask First:** 安装或替换服务器运行环境、开放公网端口、改写不兼容数据或接入非 OpenAI-compatible 模型前先询问。

**Never:** 浏览器直连 MCP/模型；Harness 共用历史或互调工具；Harness 执行 SQL、生成数据库 ID 或可信坐标；用线性节点列表冒充实验树；把“学习/了解”单独作为任务；重做现有视觉。

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| 方向不足 | 模糊目标 | 返回一个追问和最新意图 | 非法输出不推进状态 |
| 方向确认 | 信息充分 | 生成不可变确认意图 | 重复确认幂等 |
| 生成计划 | 确认意图、时长、难度 | 返回逐层增多的节点树与任务 | 校验父子关系、层宽递增、工时与无环性 |
| 创建 | 合法草稿 | 事务写入实验图 | 防重复，失败回滚 |
| 串话 | 用户或输入类型不匹配 | 拒绝且不泄露数据 | 返回 403/409 |

</frozen-after-approval>

## Code Map

- `backend/` -- Spring Boot API、MCP Client、业务模块及测试。
- `harnesses/requirements/` -- Python 需求确认 MCP Server。
- `harnesses/planner/` -- Python 树状节点规划 MCP Server。
- `database/migrations/20260911_add_experiment_creation.sql` -- 创建期状态、节点父级与结构升级。
- `app.js` -- 创建向导 API 适配。
- `compose.yaml`、`README.md` -- 本地编排、配置和部署说明。

## Tasks & Acceptance

**Execution:**
- [x] `backend/` -- 建立 Spring Boot REST API、JDBC/MySQL、MCP Client 与统一错误协议。
- [x] `harnesses/requirements/` -- 建立只输出 `StudentIntentEnvelope` 的 Python MCP Server。
- [x] `harnesses/planner/` -- 建立只接收 `ConfirmedIntent`、输出 `ExperimentTree` 的 Python MCP Server。
- [x] `backend/src/main/` -- 实现状态机、单向交接、树校验、布局和事务创建。
- [x] `database/migrations/20260911_add_experiment_creation.sql` -- 支持会话、草稿、五档难度、小时预算、阶段和 `parent_node_id`。
- [x] `app.js` -- 保留页面结构，接入对话、树状预览、确认和节点/连线渲染。
- [x] `backend/src/test/`、`harnesses/*/test_*.py` -- 覆盖隔离、树分叉、环路、并发版本与事务失败路径。
- [x] `compose.yaml`、`README.md` -- 编排三个服务，仅公开核心 API，并记录部署流程。

**Acceptance Criteria:**
- Given 并发用户与会话，when 调用 Harness，then 只能读取对应用户和职责的数据，规划器输入不存在原始对话。
- Given 已确认方向、时长与难度，when 生成计划，then 从根节点向下形成至少三层且有多个分叉的树，每节点均含按序可验收任务。
- Given 确认合法计划，when 创建，then MySQL 一次性形成实验、父子节点、任务和连线，前端按深度纵向、同级横向渲染。

## Design Notes

实验图是有根树：根节点代表起始目标，后续能力按主题不断分叉；每个小阶段是实际节点，任务显示在详情中。布局按深度从上到下，同一父节点的子节点横向展开。节点 Harness 只输出稳定 key、`parentKey`、任务和工时，坐标由后端确定。两个 Python 服务使用不同端口、配置、工具注册表和容器；只允许 Spring Boot 通过内部网络访问。模型通过可替换 Provider 接口访问，首版支持 OpenAI-compatible API；测试使用确定性假 Provider。

## Verification

**Commands:**
- `mvn -f backend/pom.xml test` -- Java API、隔离合同和事务测试通过。
- `python -m pytest harnesses` -- 两个 Harness 的合同与边界测试通过。
- `docker compose config` -- 三服务编排配置有效且 Harness 无公网端口。
- `mysql --user=<authorized-user> --password < database/verify.sql` -- 新旧结构验证通过。

## Spec Change Log

- 2026-09-11 review：执行清单原先同时写入 SSE/JPA，但冻结需求和现有三步前端均为同步请求，且原子化物化需要显式 SQL 顺序。收敛为 REST + Spring JDBC；保留统一错误、MCP、MySQL 事务与所有用户可见行为，避免为了未使用的流式通道和 ORM 重写已验证的数据边界。

## Suggested Review Order

**核心创建链路**

- 从会话确认到原子物化，集中体现单向交接与状态边界。
  [`ExperimentCreationService.java:54`](../../backend/src/main/java/com/bmhs/experimentcreation/ExperimentCreationService.java#L54)

- 远程调用后用版本复核，避免长事务和并发覆盖。
  [`CreationTransactions.java:27`](../../backend/src/main/java/com/bmhs/experimentcreation/CreationTransactions.java#L27)

**Harness 隔离与合同**

- 按唯一工具选择独立 MCP 客户端，并隔离单端故障。
  [`McpHarnessGateway.java:52`](../../backend/src/main/java/com/bmhs/experimentcreation/McpHarnessGateway.java#L52)

- 需求端只澄清确认，不生成任何节点或代码。
  [`requirements/service.py:22`](../../harnesses/requirements/service.py#L22)

- 规划端生成 1→3→6 树，并按难度调整指导程度。
  [`planner/service.py:36`](../../harnesses/planner/service.py#L36)

- 双网络让两个 Harness 只能分别连接核心后端。
  [`compose.yaml:1`](../../compose.yaml#L1)

**树校验与数据模型**

- 后端对不可信树执行层宽、环路、任务与工时校验。
  [`ExperimentTreeValidator.java:20`](../../backend/src/main/java/com/bmhs/experimentcreation/ExperimentTreeValidator.java#L20)

- 创建会话、阶段和父节点字段支撑完整落库。
  [`schema.sql:82`](../../database/schema.sql#L82)

- 增量迁移兼容旧难度约束并补齐命名索引。
  [`20260911_add_experiment_creation.sql:76`](../../database/migrations/20260911_add_experiment_creation.sql#L76)

**前端呈现与回归验证**

- 确认前按 parentKey 展示真实分叉树和节点任务。
  [`app.js:527`](../../app.js#L527)

- 确认后在画布创建节点并绘制父子连线。
  [`app.js:148`](../../app.js#L148)

- 单元测试覆盖隔离、并发、环路、工时和失败物化。
  [`ExperimentCreationServiceTest.java:25`](../../backend/src/test/java/com/bmhs/experimentcreation/ExperimentCreationServiceTest.java#L25)
