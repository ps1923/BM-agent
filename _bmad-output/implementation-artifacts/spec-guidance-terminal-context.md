---
title: '将最近终端结果纳入普通代码指导上下文'
type: 'feature'
created: '2026-10-05'
status: 'draft'
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** 普通代码指导当前读取实验节点、工作区文件、历史对话和 RAG 案例，但不读取学生最近的终端命令及输出。学生在终端运行测试后提问时，模型因此缺少最直接的失败信息，指导也无法针对实际错误给出验证建议。

**Approach:** 普通指导在现有运行实例和学生权限边界内，附带最近若干条已结束命令的命令文本、退出状态和输出尾部。上下文须限量、脱敏，并明确作为不可信数据传给模型；无终端记录时保持现有指导流程。

## Boundaries & Constraints

**Always:** 只查当前学生、当前运行工作区的已结束命令；最近记录按时间顺序呈现，输出保留尾部；命令和输出沿用秘密脱敏，记录数/单条输出/总上下文均限长；将输出标成不可信数据；不新增持久副本；日志缺失或不可读不得阻断指导。

**Ask First:** 若实现发现需要新增数据库字段/迁移、改变外部 API、把终端日志提供给教师以外的新角色，或需要向模型发送超出现有总上下文预算的内容，停止并征求用户确认。

**Never:** 不读取其他学生或其他运行实例的命令；不把命令输出拼接进 system prompt；不把未结束命令伪装为已完成；不移除既有工作区文件、对话、RAG 或敏感信息过滤；不声称启发式脱敏可以识别所有自定义密钥。

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| RECENT_FAILURES | 当前工作区有已结束命令 | 附最近有界记录及截断后的输出尾部 | 内容受总字符预算限制 |
| NO_TERMINAL_HISTORY | 当前工作区无会话或无已结束命令 | 使用现有文件、节点、对话和 RAG 上下文完成指导 | 不报错、不虚构终端信息 |
| ISOLATION | 其他运行实例或学生有更新的命令 | 不纳入当前上下文 | 同时按学生、工作区约束 |
| MISSING_OR_UNREADABLE_LOG | 命令记录存在但输出文件不可读 | 可保留安全的命令状态元数据，跳过不可读输出并继续指导 | 不泄露宿主机路径，不使指导请求失败 |

</frozen-after-approval>

## Code Map

- `backend/src/main/java/com/bmhs/workspace/TerminalRepository.java` -- 按学生和工作区查询最近已结束命令。
- `backend/src/main/java/com/bmhs/workspace/TerminalService.java` -- 安全读取有界日志，容忍缺失文件。
- `backend/src/main/java/com/bmhs/guidance/GuidanceService.java` -- 将脱敏终端上下文纳入预算和 JSON 用户消息。
- `backend/src/test/java/com/bmhs/workspace/TerminalRepositoryTest.java`、`TerminalServiceTest.java` -- 验证查询隔离、状态筛选、输出尾部和日志失败处理。
- `backend/src/test/java/com/bmhs/guidance/GuidanceServiceTest.java` -- 验证模型收到脱敏且限长的终端证据，缺少终端上下文时仍可指导。

## Tasks & Acceptance

**Execution:**
- [ ] `TerminalRepository.java` -- 查询当前学生工作区最近已结束命令 -- 隔离其他运行。
- [ ] `TerminalService.java` -- 安全读取并限长，优先保留输出尾部 -- 控制磁盘读取和模型预算。
- [ ] `GuidanceService.java` -- 脱敏并按预算传递不可信终端上下文 -- 支持依据真实错误指导。
- [ ] `TerminalRepositoryTest.java`、`TerminalServiceTest.java` -- 覆盖隔离、空历史、日志缺失和截断。
- [ ] `GuidanceServiceTest.java` -- 验证失败输出、脱敏、不可信边界和预算。

**Acceptance Criteria:**
- Given 学生在当前实验工作区运行命令并收到非零退出码，when 学生请求普通代码指导，then DeepSeek 用户上下文包含该命令、失败状态和受限/脱敏后的输出尾部。
- Given 命令输出包含指令注入文本，when 构造模型请求，then 文本只作为带边界的用户数据，不改变 system prompt。
- Given 没有终端历史、日志缺失或 RAG 不可用，when 学生请求指导，then 请求仍能按可用上下文正常降级完成。
- Given 同一学生拥有多个运行实例或系统有其他学生命令，when 查询当前运行指导上下文，then 只包含当前运行所属工作区的记录。

## Verification

**Commands:**
- `mvn -f backend/pom.xml -Dtest=TerminalRepositoryTest,TerminalServiceTest,GuidanceServiceTest test` -- expected: all focused isolation, truncation, redaction, and fallback tests pass.
- `mvn -f backend/pom.xml test` -- expected: full Spring Boot unit/Web suite passes.
- `git diff --check` -- expected: no whitespace errors.
