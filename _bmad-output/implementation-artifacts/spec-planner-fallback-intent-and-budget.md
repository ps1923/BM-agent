---
title: '规划 Harness 兜底计划贴合意图并遵守时长预算'
type: 'bugfix'
created: '2026-10-03'
status: 'in-review'
route: 'plan-code-review'
baseline_commit: '8454527b846d8add4f39178501c9fa81ac7f91a0'
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** 当 DeepSeek 不可用或输出两次不合法时，规划 Harness 会使用硬编码的画布、接口、MySQL 等任务作为兜底，即使已确认目标与这些技术无关；同时节点时长使用整除和至少一分钟的钳制，可能超过学生设定周期。模型输出的规划校验还允许最多额外 60 分钟，与 Planner Skill 的“总预计时间在预算内”相冲突。兜底也未参考学生已有基础，可能重复安排已掌握的工作。

**Approach:** 将兜底规划改为由已确认的学习目标、成果、成功标准、学生基础、约束、难度和技术栈驱动的最小端到端学习路径；在不伪造未确认技术细节的前提下，保持可练习、可验证。只将可确认的依赖表达为树关系；无法确定独立性时不臆造分支。对模型结果和兜底结果执行严格总时长校验，并按整数分钟稳定分配预算；时间不足时缩减可选阶段，而不是超预算或留下不相关阶段。

## Boundaries & Constraints

**Always:** 代码仅修改 `harnesses/planner/`；新规格存放在 `_bmad-output/implementation-artifacts/`；保留所有现有更改。只使用 ConfirmedIntent 中已确认的信息；树仍满足 ExperimentTree 合同、Planner Skill、节点单项不超过 1440 分钟且时长总和不超过输入预算。常规模型树仍保留真实多级结构与依赖。

**Ask First:** 如果无法从已确认字段构成至少一个与目标相关且能自验的最小学习成果，停止并报告原因，不得擅自补充具体技术栈或功能。

**Never:** 不修改后端、前端、共享合同、部署文件或其他 Harness；不新增依赖；不把固定画布 / Spring / MySQL 阶段作为所有实验的默认内容；不放宽时长合同以迁就旧兜底行为。

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Python CLI fallback | 已确认 Python 命令行工具目标，模型失败 | 兜底节点围绕该目标、其成功标准及 Python 组织，不出现无关画布或 MySQL 任务 | 返回合法 ExperimentTree |
| Minimal duration | 正整数预算小于常规阶段数 | 缩减阶段数仍保留单一根节点及与目标相关的可验证结果，分钟总和不超预算 | 若不能保持可验证成果则明确失败，不返回误导计划 |
| Remainder allocation | 预算不能被节点数整除 | 分配整数分钟并处理余数，节点总和不超预算 | 无 |
| Model over budget | 模型树超过用户预算但低于当前宽松容差 | 校验拒绝并进入已有修复 / 安全兜底流程 | 修复失败时兜底仍须严格满足预算 |
| Large duration | 总周期远大于单节点上限 | 每节点不超过 1440 分钟，节点总和不超过总周期 | 无 |

</frozen-after-approval>

## Code Map

- `harnesses/planner/service.py` -- 模型规划校验、模型失败修复路径、确定性兜底树和预算分配。
- `harnesses/planner/test_service.py` -- 规划 Harness 的合同、异常恢复、意图相关性和预算边界测试。
- `harnesses/planner/skills/node-planning.md` -- 一次性规划与学习者视角的验收规范；仅在需澄清其预算措辞时调整。

## Tasks & Acceptance

**Execution:**
- [x] `harnesses/planner/service.py` -- 移除兜底阶段对画布 / 接口 / MySQL 的固定假设；根据确认成果、成功标准、学生基础、约束、难度和已确认技术栈组织有针对性的最小学习路径，并处理短周期、余数分配及单节点上限。
- [x] `harnesses/planner/service.py` -- 将模型规划的总时间校验改为严格不超过给定预算；确保修复及兜底路径均应用同一预算规则，保留现有结构验证和日志安全性。
- [x] `harnesses/planner/test_service.py` -- 添加与目标相关性、短周期、不可整除周期、极长周期、模型超预算与修复失败场景的回归测试，并保留多级节点测试。

**Acceptance Criteria:**
- Given 已确认成果是 Python 命令行工具且要求 Python，when 模型调用两次失败，then 兜底树的产物、任务和验收围绕该成果，不包含无关技术栈专属任务。
- Given 学生已具备目标技术栈经验或没有确认技术栈，when 生成兜底树，then 提示和环境任务尊重已知基础；不把未经确认的框架、数据库或页面作为前置条件。
- Given 已确认的验收标准表达多个独立产物，when 兜底规划能够确认其依赖，then 树关系体现真实学习路径；依赖不明时不得伪造可提前解锁的兄弟分支。
- Given 任意正整数时长，when 生成模型树或兜底树，then 每个节点为正整数分钟、单节点不超过 1440 分钟、总和不超过输入周期。
- Given 可用周期不足以保留通常阶段数，when 构建兜底计划，then 计划缩减可选阶段并保留至少一个真实、可自验的目标成果；不满足此条件时明确失败，不伪造成功计划。
- Given 模型树超预算，即使只超过不足 60 分钟，when 校验，then 拒绝该结果并尝试现有修复 / 安全兜底流程。
- Given 合法的多级模型树在预算内，when 校验，then 保留其层级、父子依赖及节点顺序。

## Spec Change Log

## Design Notes

兜底不是另一个硬编码的项目模板：它应从 ConfirmedIntent 的可观察成果和验收标准构造最小端到端学习路径；仅在已确认时才使用 required_stack。清单中的每条 success criterion 作为单独可检查的成果节点，共用目标 / 环境前置节点，不臆造彼此依赖或增加无汇合能力支持的最终节点。预算不足时按清单顺序取可支持的前置成果，并在 purpose 和未完成任务中标明遗漏数；不静默宣称整份清单已达成。兜底节点按 15 分钟为最小可用学习时段收缩数量，低于 15 分钟时拒绝生成；已确认运行环境时才跳过环境准备，已有文件素材本身不证明环境就绪。预算分配必须先确定可保留的节点，再对整数分钟余数作确定性分配，避免轮次间相同输入产生不同树。不能为了满足分钟数字而生成只有形式、无法学习或验证的阶段。

## Verification

**Commands:**
- `.venv/Scripts/python.exe -m pytest harnesses/planner/test_service.py -q` -- expected: all planner tests pass.
- `.venv/Scripts/python.exe -m pytest harnesses -q` -- expected: all Harness tests pass.
