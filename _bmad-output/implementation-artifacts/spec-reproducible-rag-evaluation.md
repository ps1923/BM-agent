---
title: '复核并规范 RAG 对比实验结果'
type: 'feature'
created: '2026-10-01'
status: 'done'
baseline_commit: '5f510e46f87dee4549de9216e501ab7cd738a048'
context:
  - '{project-root}/backend/src/test/resources/rag/benchmark-cases.json'
  - '{project-root}/_bmad-output/implementation-artifacts/rag-ab-results-20260919.json'
  - '{project-root}/_bmad-output/implementation-artifacts/deferred-work.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** 仓库保存了 20 个 Bug 的 DeepSeek 有/无 RAG 回答及指标，但没有工具复核分数。`seconds` 是回答生成耗时，关键词覆盖也不等于回答正确或学生解决问题；混淆口径会让论文结论超过证据。

**Approach:** 增加只读现有基准集与原始结果的确定性分析器，校验样例配对并重算 Top-3 命中、关键词覆盖和模型回答耗时；不调用模型。同步写明计算口径和现有证据边界。

## Boundaries & Constraints

**Always:** 原始 JSON 只读；仅用 Python 标准库；校验样例 ID 完整且唯一。关键词按大小写不敏感的子串匹配，先算逐样例命中比例再取算术平均；Top-3 命中指 `eval-{caseId}` 出现在 `retrievedIds`。显示公式；无效输入或存档分数不符时非零退出并指出样例。

**Ask First:** 更改基准问题、gold keywords、既有原始回答或实验结论；重新调用 DeepSeek、Chroma 或其他外部服务生成对照结果。

**Never:** 修改或覆盖原始 JSON；把回答生成延迟称为学生 Bug 解决耗时；把关键词命中率称为回答准确率、解决成功率或因果效果；伪造双盲/人工评分、学生行为或统计显著性。

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|----------------------------|----------------|
| VALID_DATA | 当前基准与原始结果 | 输出两种条件的 Top-3、平均/全关键词覆盖率及回答生成耗时，核对存档分数 | 不一致时列出样例并非零退出 |
| INVALID_DATA | 缺文件、ID 重复/缺失、空关键词、回答/数值非法 | 不生成成功汇总 | 指出文件与样例；输入保持不变 |

</frozen-after-approval>

## Code Map

- `scripts/evaluate_rag_benchmark.py` -- 验证输入、重算指标并输出报告；默认读取本工单两份数据，支持参数指定路径。
- `tests/test_rag_benchmark_eval.py` -- 覆盖有效基准及错误输入。
- `backend/src/test/resources/rag/benchmark-cases.json`、`_bmad-output/implementation-artifacts/rag-ab-results-20260919.json` -- 唯一评分标准和只读原始数据。
- `_bmad-output/implementation-artifacts/deferred-work.md` -- 实验口径和证据边界。

## Tasks & Acceptance

**Execution:**
- [x] `scripts/evaluate_rag_benchmark.py` -- 用标准库校验数据并重算指标；支持文本/JSON 输出及输入路径参数，不调用外部服务或写入源文件。
- [x] `tests/test_rag_benchmark_eval.py` -- 验证有效数据、评分一致和缺失/重复/非法输入失败。
- [x] `_bmad-output/implementation-artifacts/deferred-work.md` -- 记录计算公式和复核数据，区分回答生成延迟、词面覆盖与学生解决效果。

**Acceptance Criteria:**
- Given 现存 20 个案例与两组结果，when 运行分析器，then 校验 ID 并报告 Top-3 20/20、平均覆盖无 RAG 0.6833/有 RAG 0.9000、全关键词命中 8/20 与 15/20、平均回答生成耗时 1.658/1.597 秒，且与存档分数一致。
- Given 数据缺失、重复或分数不符，when 运行分析器，then 非零退出并指出样例，不修改输入。
- Given 结果含 `seconds`，when 汇总，then 标为模型回答生成耗时，不声称学生解题耗时或答案正确率。

## Verification

**Commands:**
- `.venv/Scripts/python.exe scripts/evaluate_rag_benchmark.py` -- prints a successful, deterministic report for the repository data.
- `.venv/Scripts/python.exe -m unittest discover -s tests -p test_rag_benchmark_eval.py` -- all evaluator tests pass.
- Re-run the analyzer twice -- reports are byte-identical and source JSON hashes remain unchanged.

## Verification Record (2026-10-01)

- 21 evaluator tests pass, including invalid UTF-8/JSON, duplicate keys/IDs, missing/extra samples, invalid metrics, CLI error paths and source preservation.
- Text/JSON output is deterministic; current source data hashes remained unchanged across repeated reports.
- Independent review findings were patched: strict duplicate-key rejection, finite overflow-safe aggregation, UTF-8 errors, title/error type validation, explicit displayed formulas, and source paths in data validation errors.

## Suggested Review Order

**执行入口与指标校验**

- 先看命令行入口如何解析路径、格式和错误码。
  [`evaluate_rag_benchmark.py:268`](../../scripts/evaluate_rag_benchmark.py#L268)

- 再看配对校验、Top-3、覆盖率和均值计算。
  [`evaluate_rag_benchmark.py:130`](../../scripts/evaluate_rag_benchmark.py#L130)

- 检查回答分数复算、耗时验证和溢出防护。
  [`evaluate_rag_benchmark.py:106`](../../scripts/evaluate_rag_benchmark.py#L106)

- 查看重复 JSON 键、非法编码和解析错误处理。
  [`evaluate_rag_benchmark.py:24`](../../scripts/evaluate_rag_benchmark.py#L24)

**论文指标解释**

- 检查控制台报告是否明确展示公式和结论限制。
  [`evaluate_rag_benchmark.py:235`](../../scripts/evaluate_rag_benchmark.py#L235)

- 对照固定样例结果和论文可支持的结论边界。
  [`deferred-work.md:52`](deferred-work.md#L52)

**回归测试**

- 核实复算值与 20 个当前案例的期望结果。
  [`test_rag_benchmark_eval.py:49`](../../tests/test_rag_benchmark_eval.py#L49)

- 检查异常输入、CLI 错误路径和源文件只读保证。
  [`test_rag_benchmark_eval.py:139`](../../tests/test_rag_benchmark_eval.py#L139)
