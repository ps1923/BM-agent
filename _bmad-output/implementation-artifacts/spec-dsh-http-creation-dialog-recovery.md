---
title: '修复 DSH HTTP 访问与实验创建恢复体验'
type: 'feature'
created: '2026-10-22'
status: 'done'
baseline_commit: 'b3f4bb4877ad22d0eb23ef7ac4df33559923ead5'
context:
  - '{project-root}/app.js'
  - '{project-root}/index.html'
  - '{project-root}/styles.css'
  - '{project-root}/backend/src/main/java/com/bmhs/experimentcreation'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** DSH 启动地址在前端被强制要求 HTTPS，公网 HTTP 演示无法打开；实验创建回复和等待体验生硬，关闭后草稿无法恢复，长方案清单会挤出确认按钮。

**Approach:** 允许来自平台且主机匹配、无内嵌凭据的 HTTP(S) DSH 地址；将 Harness 可见原文、动态等待、周期和难度表单组织为连续创建流程。关闭只隐藏弹窗，新增按用户隔离的草稿及消息读取能力，并令方案清单独立滚动、确认操作固定可见。

## Boundaries & Constraints

**Always:** 不展示隐藏推理或工具内部内容；不伪造逐字流式输出；HTTP 仅用于个人毕设演示，明文传输风险已知；DSH 地址必须匹配平台来源及专属 DSH 路由；关闭或刷新不得丢失已保存草稿；所有草稿与消息读取必须校验当前用户；复用现有会话、消息及时间字段，不清理历史数据；保留周期和难度表单及已有实验创建契约。

**Ask First:** 无；按本次已确认方案执行。

**Never:** 不新增本地桥接器；不自动跳转 HTTPS；不因遮罩点击关闭或取消会话；不把静态占位文案伪装成 Harness 原话；不让方案内容挤出确认按钮；不删除或覆盖服务器历史版本、数据库数据或运行工作区。

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| HTTP DSH launch | 平台返回同源的公网 HTTP DSH 路由 | 在新标签页打开，不做 HTTPS 跳转 | 拒绝来源、端口、路径不匹配或含凭据地址 |
| Draft recovery | 当前用户未物化会话 | 草稿列表展示状态和更新时间；继续时恢复有序消息、方向、周期、难度及方案 | 其他用户或未知会话返回未找到 |
| Failed turn | 用户消息请求失败 | 服务端保存用户消息，草稿恢复后仍可见并可重试，不伪造 Harness 回复 | 同一待处理消息重试不重复写入 |
| Long plan | 节点超过弹窗可视高度或窄屏 | 节点区独立滚动，确认按钮始终可见且可操作 | 页面缩放/小视口仍可滚动到内容和操作 |

</frozen-after-approval>

## Code Map

- `app.js` -- DSH 地址校验、创建会话生命周期、对话/等待、草稿恢复及方案预览交互。
- `index.html` -- 实验库草稿区、连续创建界面及固定操作区结构。
- `styles.css` -- 对话状态、弹窗布局、方案独立滚动和窄屏适配。
- `backend/src/main/java/com/bmhs/experimentcreation/ExperimentCreationController.java` -- 用户会话草稿及有序消息读取路由。
- `backend/src/main/java/com/bmhs/experimentcreation/CreationRepository.java` -- 基于现有数据查询用户草稿和消息。
- `backend/src/main/java/com/bmhs/experimentcreation/ExperimentCreationService.java` -- 草稿过滤、会话所有权和消息读取。
- `tests/dsh-launch-ui.test.js`, `tests/api-config.test.js` -- DSH HTTP 与创建 UI 客户端回归。
- `backend/src/test/java/com/bmhs/experimentcreation` -- 草稿查询、消息排序及用户隔离测试。

## Tasks & Acceptance

**Execution:**
- [x] `app.js` -- 校验平台来源和 DSH 专属路径；实现连续对话、动态等待、失败重试、关闭保留及草稿恢复。
- [x] `index.html` -- 在实验库加入未完成草稿列表，并将方向、周期、难度、方案组织为连贯对话流程。
- [x] `styles.css` -- 让弹窗内容和方案节点区受限滚动，确认按钮固定可见，适配窄屏和小窗口。
- [x] `ExperimentCreationController.java`, `CreationRepository.java`, `ExperimentCreationService.java` -- 提供用户隔离草稿与有序消息读取，并限制单次列表量。
- [x] 前端及后端测试 -- 覆盖 HTTP 校验、关闭/Esc、失败重试、草稿恢复与跨用户拒绝、长清单布局。

**Acceptance Criteria:**
- Given 平台返回公网 HTTP DSH 地址，when 学生启动工作区，then 浏览器打开同源专属 DSH 地址且不跳转 HTTPS；来源、端口、路径不匹配或 URL 含内嵌凭据时拒绝。HTTP 明文传输仅作为个人演示风险接受。
- Given 学生发送方向消息，when Harness 请求处理中，then 立即显示用户消息和动态等待；成功后显示完整可见回复，失败后保留内容并能重试。
- Given 创建处于任意未完成阶段，when 点击关闭、遮罩或按 Esc 后重新进入草稿，then 已保存阶段、消息和方案恢复；遮罩本身不关闭，已创建实验不出现在草稿中。
- Given 长节点树或窄窗口，when 打开方案确认，then 内容在内部滚动且确认按钮始终可操作。
- Given 另一用户猜测草稿会话 ID，when 请求草稿或消息，then 不得读取该会话内容。

## Spec Change Log

## Design Notes

会话关闭是纯展示状态变化，不等于取消服务端会话。接口只返回当前用户拥有且尚未创建实验的草稿，具体活跃状态由现有会话状态字段表达；消息按数据库创建时间及稳定 ID 顺序返回。普通 HTTP 不保证浏览器开放所有安全上下文能力。用户选择以个人演示方式继续使用 HTTP；需清楚提示其明文传输风险，不擅自扩展为 HTTPS 或本机代理。

## Verification

**Commands:**
- `node --test tests/dsh-launch-ui.test.js tests/api-config.test.js` -- expected: all DSH and creation UI regression tests pass.
- `mvn -f backend/pom.xml test` -- expected: backend unit and web tests pass.
- `node --test tests/*.test.js` -- expected: full browser-side regression suite passes.

**Manual checks:** 在 HTTP 演示地址实际启动 DSH，核对 Harness 完整回复、等待状态、草稿恢复、身份隔离提示及长清单确认操作；如果浏览器阻止 DSH 必要能力，记录具体能力和浏览器限制，不擅自引入替代部署方案。

## Suggested Review Order

**DSH 地址边界与学生入口**

- 入口先走原生 DSH 启动；不再创建旧 BM-HS 工作区。
  [`app.js:3059`](../../app.js#L3059)

- HTTP 演示只接受平台来源和专属 DSH 路由。
  [`app.js:385`](../../app.js#L385)

**创建对话与恢复**

- 方向消息先持久化，失败或刷新后仍可恢复。
  [`app.js:3916`](../../app.js#L3916)

- 消息恢复保持会话、用户与排序边界。
  [`CreationRepository.java:63`](../../backend/src/main/java/com/bmhs/experimentcreation/CreationRepository.java#L63)

- 失败消息幂等保留，成功回复才更新方向状态。
  [`CreationTransactions.java:27`](../../backend/src/main/java/com/bmhs/experimentcreation/CreationTransactions.java#L27)

- 弹窗退出只隐藏视图，不取消草稿。
  [`app.js:4092`](../../app.js#L4092)

**页面布局与回归证据**

- 方案清单置于滚动区，确认按钮独立固定。
  [`index.html:166`](../../index.html#L166)

- HTTP、草稿、失败重试与页面入口由回归测试覆盖。
  [`tests/dsh-launch-ui.test.js:1`](../../tests/dsh-launch-ui.test.js#L1)
