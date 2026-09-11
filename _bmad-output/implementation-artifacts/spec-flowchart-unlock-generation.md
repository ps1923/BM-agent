---
title: '将实验计划改为可解锁的流程图节点'
type: 'feature'
created: '2026-09-11'
baseline_commit: 'NO_VCS'
status: 'done'
context:
  - '{project-root}/_bmad-output/implementation-artifacts/spec-integrate-isolated-harnesses.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-recover-harness-structured-output.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** 当前 Planner 生成的是拆分任务清单；`parentKey` 没有明确表达“完成前置节点后才能解锁后续节点”的流程语义。

**Approach:** 每个节点是一个有明确产出的阶段关卡，`tasks` 是完成该关卡所需的连续动作。节点数量、层级和分支由实验真实工作量与技术依赖决定，不使用固定节点数量或强行扩展；只有存在真实并行路线时才分支。下一节点必须使用前一节点已完成的产物继续推进。

## Boundaries & Constraints

**Always:** 需求 Harness 只确认需求；Planner 只接收 ConfirmedIntent；每个节点至少 3 个具体任务并描述一个完整小阶段；节点必须有可验收产出；根节点无前置，后续节点依赖真实前置产物；无环且可从根到达；保留 `node_dependencies` 落库关系。

**Ask First:** 跨分支合并依赖、DAG 布局或任务级解锁另行确认。

**Never:** 不把 tasks 当作独立画布节点；不让 Planner 访问聊天记录或生成代码；不在本阶段实现 loading、缩放或一键排布 UI；不改变两个 Harness 的会话隔离。

## I/O & Edge-Case Matrix

| Scenario | Expected Output / Behavior | Error Handling |
|---|---|---|
| 正常生成 | 按实验工作量生成自然的流程树；非根节点依赖真实前置节点，完成全部任务后解锁 | 校验预算、层级和无环 |
| 模型缺少依赖字段 | 一次修复；失败返回含完整规则的安全树 | 不返回不完整结构 |
| 错误 key/依赖 | 修复；仍失败回退确定性流程树 | 保持职责内错误 |
| 时长极短 | 保留完整结构并压缩单节点工时 | 非正时长拒绝 |

</frozen-after-approval>

## Code Map

- `harnesses/common/contracts.py` -- 节点规则合同。
- `harnesses/planner/service.py` -- 按真实工作量生成流程树和回退。
- `backend/src/main/java/com/bmhs/experimentcreation/CreationModels.java` -- 新字段及旧构造兼容。
- `backend/src/main/java/com/bmhs/experimentcreation/ExperimentTreeValidator.java` -- 依赖校验并移除固定层宽约束。
- `backend/src/main/java/com/bmhs/experimentcreation/TreeLayoutService.java` -- 布局保留关系。
- `harnesses/planner/test_service.py`、`backend/src/test/**` -- 回归测试。

## Tasks & Acceptance

**Execution:**
- [x] `harnesses/common/contracts.py` -- 添加节点解锁/完成字段。
- [x] `harnesses/planner/service.py` -- 按技术依赖生成自然流程树，禁止为凑数量制造节点。
- [x] `backend/src/main/java/com/bmhs/experimentcreation/CreationModels.java` -- 扩展字段并兼容旧构造。
- [x] `backend/src/main/java/com/bmhs/experimentcreation/ExperimentTreeValidator.java` -- 校验依赖和规则，移除“每层必须比上一层多”的人工数量门槛。
- [x] `harnesses/planner/test_service.py`、`backend/src/test/**` -- 覆盖结构和兼容性。

**Acceptance Criteria:**
- Given 合法确认需求，when Planner 生成方案，then 每个节点都是一个有明确产出的完整小阶段，包含 3–7 个具体任务，节点数量随实验工作量变化而变化。
- Given 任一非根节点，when 读取其结构，then `prerequisite_keys` 指向真实前置产物，`unlock_rule` 表示所有前置节点完成，`completion_rule` 表示本节点全部任务完成。
- Given 方案落库，when 查询节点和依赖，then `node_dependencies` 与每个 `parentKey` 一一对应，未产生任务级画布节点。
- Given 模型返回缺失或错误依赖，when 合同修复仍失败，then Harness 返回包含完整解锁字段的确定性流程树，不返回 MCP tool error。
- Given 一个只有线性前后依赖的实验，when Planner 生成方案，then 允许自然线性流程，不为满足层宽规则额外制造分支。

## Design Notes

节点是流程图关卡，不是 tasks 的独立节点。例如第一个节点可包含“创建画布、创建可拖动气泡、点击后显示右侧详情卡片”；下一个节点应承接这个产物，包含“创建顶部分区导航、按导航分块展示卡片、确定卡片数据来源、连接数据库”。只有实验确实需要同时推进互不依赖的路线时才出现同层分支。

## Verification

**Commands:**
- `.venv\\Scripts\\python.exe -m pytest harnesses -q` -- expected: all Harness contract and fallback tests pass.
- `C:\\Users\\13607\\.m2\\wrapper\\dists\\apache-maven-3.9.9\\3477a4f1\\bin\\mvn.cmd -q -f backend/pom.xml test` -- expected: Java validator, layout, gateway and service tests pass.
- `.venv\\Scripts\\python.exe -m harnesses.mcp_smoke` -- expected: planner returns a structured dependency flow with no artificial width requirement.

## Suggested Review Order

**流程生成语义**

- 按工作量信号裁剪阶段，避免为凑数量制造节点。
  [`service.py:40`](../../harnesses/planner/service.py#L40)

- 将每个阶段输出为可解锁的完整关卡，并保留真实父子依赖。
  [`contracts.py:89`](../../harnesses/common/contracts.py#L89)

**后端合同与兼容**

- 校验依赖规则、自然线性流程和父节点可达性。
  [`ExperimentTreeValidator.java:20`](../../backend/src/main/java/com/bmhs/experimentcreation/ExperimentTreeValidator.java#L20)

- 传递新解锁字段，同时为旧计划提供默认规则。
  [`CreationModels.java:75`](../../backend/src/main/java/com/bmhs/experimentcreation/CreationModels.java#L75)

- 布局只计算坐标，不把任务拆成画布节点。
  [`TreeLayoutService.java:39`](../../backend/src/main/java/com/bmhs/experimentcreation/TreeLayoutService.java#L39)

**回归验证**

- 覆盖工作量裁剪、解锁字段和非法父节点。
  [`test_service.py:27`](../../harnesses/planner/test_service.py#L27)

- 真实 MCP 调用验证结构化依赖字段。
  [`mcp_smoke.py:46`](../../harnesses/mcp_smoke.py#L46)

