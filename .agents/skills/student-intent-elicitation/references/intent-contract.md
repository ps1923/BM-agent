# StudentIntentEnvelope 合同

输出一个结构化对象。对话界面可以展示 `assistant_message` 和 `next_question`，Harness 保存其余字段作为会话状态。

```json
{
  "schema_version": "0.1",
  "status": "needs_clarification | ready_for_confirmation | confirmed",
  "assistant_message": "面向学生的简短回复",
  "intent": {
    "raw_request": "学生的原始目标",
    "observable_outcome": "学习结束后能够做什么",
    "target_artifact": "需要产出的项目、功能、解释或演示",
    "current_state": {
      "known_skills": [],
      "relevant_experience": "",
      "starting_assets": []
    },
    "constraints": {
      "time_budget_minutes": null,
      "deadline": null,
      "required_stack": [],
      "environment": [],
      "excluded_scope": []
    },
    "preferences": {
      "difficulty": null,
      "guidance_level": null,
      "language": "zh-CN"
    },
    "success_criteria": []
  },
  "field_evidence": {
    "observable_outcome": "explicit | inferred | missing",
    "target_artifact": "explicit | inferred | missing",
    "current_state": "explicit | inferred | missing",
    "time_budget": "explicit | inferred | missing",
    "success_criteria": "explicit | inferred | missing"
  },
  "missing_critical_fields": [],
  "assumptions": [],
  "next_question": null
}
```

需要追问时，`next_question` 使用以下结构：

```json
{
  "id": "stable-question-id",
  "text": "一次只问一个主问题",
  "answer_type": "free_text | single_select | multi_select",
  "options": [
    { "value": "api", "label": "完成可运行的登录 API" }
  ],
  "why_it_matters": "该答案将影响哪些设计决策"
}
```

## 状态判定

`needs_clarification`：至少一个关键缺口会改变实验范围、路径、脚手架或验收方式。

`ready_for_confirmation`：已经能够形成合理实验设计，但学生还没有确认 Harness 展示的理解摘要。

`confirmed`：学生明确确认当前摘要，或明确要求按所列默认假设继续。

## 最低充分信息

进入 `ready_for_confirmation` 前，至少应知道：

- 一个可观察的目标，而不只是技术名词。
- 学生的大致起点，足以决定讲解和代码脚手架程度。
- 可用时间或可接受的范围边界。
- 至少一个能够验证结果的成功标准。

`target_artifact`、指定技术栈和环境只在目标需要时才是必填项。例如理解概念可以通过解释、比较或小型演示验收，不必强制创建完整项目。

## 字段证据

- `explicit`：学生或可信的界面状态直接给出。
- `inferred`：为保持流程顺畅而提出的暂定理解，必须同时列入 `assumptions`。
- `missing`：没有足够依据。

不要输出伪精确的总体置信度数字。字段证据比单一分数更容易审查和修正。
