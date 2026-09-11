---
title: '修复 Harness 结构化输出偶发失败'
type: 'bugfix'
created: '2026-09-11'
status: 'done'
baseline_commit: 'NO_VCS'
context:
  - '{project-root}/_bmad-output/implementation-artifacts/spec-integrate-isolated-harnesses.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** 特定对话中，真实模型连续两次返回不满足 Pydantic 合同的 JSON，Python Harness 以 MCP tool error 结束；Java 又把 tool error 和空结构统一显示成“未返回有效的结构化结果”，创建流程因此中断。

**Approach:** 真实模型仍为首选；一次模型修复失败后，由对应 Harness 返回职责范围内、可验证的安全兜底结果。Java 区分工具失败和结构缺失，并只记录脱敏诊断信息。

## Boundaries & Constraints

**Always:** 两个 Harness 继续隔离；兜底结果必须通过现有合同和树校验；需求端只澄清/确认，规划端只生成树；日志不得包含凭据、完整对话或模型原文。

**Ask First:** 更换供应商、降低树约束、修改数据库或删除会话前确认。

**Never:** 把非法 JSON 传给前端、混用两个 Harness 上下文、无限重试 API。

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| 首次合法 | 完整合同 | 验证后返回 | 不修复 |
| 一次修复成功 | 首次非法、修复合法 | 返回修复结果 | 最多一次修复调用 |
| 连续非法 | 两次均非法 | 返回安全兜底合同 | 脱敏 warning |
| 供应商故障 | HTTP、鉴权、超时 | 明确调用失败 | 不伪装成格式兜底 |
| 结构缺失 | MCP 成功但无 structuredContent | 明确结构缺失 | 记录工具名和类别 |

</frozen-after-approval>

## Code Map

- `harnesses/requirements/service.py` -- 需求验证、修复与安全兜底。
- `harnesses/planner/service.py` -- 实验树验证、预算检查与安全兜底。
- `backend/src/main/java/com/bmhs/experimentcreation/McpHarnessGateway.java` -- MCP 结果分类和脱敏日志。
- `harnesses/*/test_service.py`、`backend/src/test/**` -- 回归测试。

## Tasks & Acceptance

**Execution:**
- [x] `harnesses/requirements/service.py` -- 捕获第二次合同失败，基于当前意图返回单一追问或确认结果。
- [x] `harnesses/planner/service.py` -- 捕获第二次合同/预算失败，返回满足分叉、任务和工时限制的安全树。
- [x] `McpHarnessGateway.java` -- 区分 tool error、空结构和反序列化错误，增加脱敏日志。
- [x] Python/Java 测试 -- 覆盖连续非法输出、角色违规、超预算和 MCP 结果分类。
- [x] 重启本地服务，复测等价对话与真实 MCP smoke。

**Acceptance Criteria:**
- Given 模型连续两次返回非法合同，when 工具执行，then MCP 仍返回职责内且可验证的 structuredContent。
- Given 供应商 HTTP 错误或超时，when 调用 Harness，then 后端明确报告调用失败且不泄露敏感信息。
- Given 用户沿“mysql → 能用 → 增删改查 → html → Java Spring Boot”继续，when 提交回答，then 返回下一条有效需求状态。

## Design Notes

兜底只处理“模型已响应但合同连续不合法”；网络、鉴权、限流和超时仍显式失败。需求兜底沿用当前已验证意图，规划兜底沿用确定性树生成器，最终仍经过 Java 校验。

## Verification

**Commands:**
- `.venv\\Scripts\\python.exe -m pytest harnesses` -- Harness 合同与兜底测试通过。
- `mvn -f backend/pom.xml test` -- 网关分类与创建链路测试通过。
- `.venv\\Scripts\\python.exe -m harnesses.mcp_smoke` -- 两个真实 MCP 工具返回 structuredContent。

## Suggested Review Order

**Harness 兜底与职责边界**

- 连续合同失败后返回可验证的需求结果，不生成节点。
  [`service.py:123`](../../harnesses/requirements/service.py#L123)

- 规划失败回退到满足树约束的安全计划。
  [`service.py:124`](../../harnesses/planner/service.py#L124)

**MCP 错误分类**

- 将工具失败、空结果和非法结构映射为可诊断错误。
  [`McpHarnessGateway.java:83`](../../backend/src/main/java/com/bmhs/experimentcreation/McpHarnessGateway.java#L83)

- 空 structuredContent 不再被宽泛反序列化异常覆盖。
  [`McpHarnessGateway.java:97`](../../backend/src/main/java/com/bmhs/experimentcreation/McpHarnessGateway.java#L97)

**回归验证**

- 连续非法模型输出、确认保护和规划预算均有覆盖。
  [`test_service.py:18`](../../harnesses/requirements/test_service.py#L18)

- Java 网关结果分类测试覆盖空结构和工具错误。
  [`McpHarnessGatewayTest.java:31`](../../backend/src/test/java/com/bmhs/experimentcreation/McpHarnessGatewayTest.java#L31)
