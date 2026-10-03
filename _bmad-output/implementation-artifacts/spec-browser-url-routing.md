---
title: '为学习工作台增加可直达的浏览器路由'
type: 'feature'
created: '2026-09-30'
status: 'in-review'
baseline_commit: '5f510e46f87dee4549de9216e501ab7cd738a048'
context:
  - '{project-root}/README.md'
  - '{project-root}/app.js'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** 当前登录、角色工作台、实验库、画布和编辑器只是同一文档中的视图切换。地址栏无法表示当前位置，浏览器前进/后退和复制链接直达均不可用，刷新也不能可靠恢复指定页面。

**Approach:** 在现有静态单页应用上增加基于 URL hash 的路由，覆盖登录、学生端、教师端、实验库、实验画布和已有运行实例的编辑器。将现有按钮导航统一接入路由，并在启动时通过服务端会话和运行实例接口恢复目标视图。

## Boundaries & Constraints

**Always:** 保留现有视觉布局和业务操作；URL 不包含凭据或会话令牌；登录状态仍由 HttpOnly Cookie 和 `/api/me` 决定；编辑器深链必须读取当前用户有权访问的运行实例；浏览器历史切换不得重复压入历史记录；静态服务器无需配置路径重写。

**Ask First:** 无。

**Never:** 不新增后端路由或数据库字段；不把客户端路由当作权限控制；打开或刷新已有编辑器链接时不得创建新的实验运行实例；不移除现有课程、实验创建、工作区、画布和编辑器功能。

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| DIRECT_ROUTE | 已登录用户打开有效角色、实验或运行 URL | 恢复对应视图；编辑器读取现有运行实例 | 无效资源回到当前角色首页并显示可理解提示 |
| AUTH_REQUIRED | 未登录用户打开受保护 URL | 验证会话后显示目标页 | 会话失效时显示登录页；成功登录后恢复安全的目标地址 |
| ROLE_MISMATCH | 学生打开教师路由或教师打开学生专属路由 | 回到本角色工作台 | 不加载或泄露受限页面数据 |
| HISTORY_NAVIGATION | 在页面间跳转后使用前进/后退 | URL 与显示视图同步，且不产生循环跳转 | 路由恢复失败时转到安全首页 |
| REFRESH | 刷新课程、实验画布或编辑器链接 | 会话有效时恢复相同位置和已有运行实例 | 服务端拒绝或资源不存在时清理失效路由并提示 |

</frozen-after-approval>

## Code Map

- `app.js` -- 路由解析、历史同步、认证守卫、实验/运行数据恢复及现有导航事件。
- `index.html` -- 确保路由入口与现有视图语义兼容；不拆分为多份 HTML。
- `styles.css` -- 仅在路由恢复需要时处理加载或不可用状态，不改写既有视觉主题。
- `backend/src/main/java/com/bmhs/workspace/WorkspaceController.java` -- 复用 `GET /runs/{runId}`，不得改变接口。

## Tasks & Acceptance

**Execution:**

- [x] `app.js` -- 建立 hash 路由表、解析与渲染单向流程；支持 `#/login`、`#/student`、`#/teacher`、`#/library`、`#/experiments/{id}/canvas`、`#/runs/{id}/editor`。
- [x] `app.js` -- 将登录成功、工作台入口、实验打开、画布/编辑器返回、退出登录接入历史导航；处理 `hashchange` 和重复导航。
- [x] `app.js` -- 页面启动先恢复服务端会话，再校验角色；实验画布通过现有实验接口加载，编辑器通过现有运行、文件和终端接口恢复已有运行。
- [x] `app.js` -- 为无效、失效、越权路由提供安全回退和页面内提示，不暴露其他用户数据；未登录时仅将白名单保护路由写入 `sessionStorage`，登录成功后恢复并消费目标，刷新登录页仍保留目标。
- [x] `index.html` -- 为动态路由加载提示和错误提示提供可访问状态区域；复用现有页面结构。

**Acceptance Criteria:**

- Given 教师或学生已登录，when 在工作台、实验库、画布和编辑器间操作，then 地址栏反映当前页面且浏览器前进/后退逐步恢复之前页面。
- Given 用户复制一个有效实验画布或已有运行编辑器地址并刷新，when 服务端确认会话与资源权限，then 页面恢复相同实验或运行实例且不创建重复运行。
- Given 未登录或角色不匹配用户打开受保护链接，when 路由初始化完成，then 页面安全回退，业务 API 仍执行服务端权限校验。
- Given URL 中的资源编号无效、已删除或无权访问，when 页面尝试加载，then 清除无效路由状态、给出明确提示并回到角色首页。

## Verification

**Commands:**
- `node --check app.js` -- expected: no syntax errors.

**Manual checks:**
- 用真实浏览器验证直接打开、刷新、前进、后退、退出登录和角色越权路由。
- 验证编辑器刷新后仍使用同一 `runId`，文件和终端恢复成功，且没有额外创建运行实例。
- 在静态服务器的项目子路径部署下验证 hash 路由无需服务端重写。

## Verification Record (2026-10-01)

- `node --check app.js`：通过。
- `python -m pytest harnesses -q`：17 项通过。
- `git diff --check`：通过；仅提示现有工作区文件的 LF/CRLF 转换警告。
- 本机静态页面 + 本地模拟 API：数字实验画布直达、已有编辑器直达与刷新、编辑器/画布前进后退、学生访问教师路由、无效数字实验回退均通过；浏览器控制台无错误。
- 模拟 API 统计：多次刷新与历史导航后 `runCreates` 保持 1；修复后 `terminalSessions` 在首次恢复创建 1 次，后续刷新/历史恢复保持不变。
- 尚未验证：未登录深链接后登录回跳、真实后端权限和子路径部署；当前本地模拟 API 的 `/me` 固定返回学生身份。后续通过本机 Maven 安装路径重跑了 Java 全套测试，结果见下方复验记录。
- 复审发现静态示例实验仍是前端演示数据，不能创建真实运行实例；已记录为实验库真实数据接入的后续工单，本工单不删除现有预览内容。
- 2026-10-01 当前工作区浏览器复验：`#/experiments/spring-init/canvas` 与 `#/experiments/999/canvas` 均回到学生工作台并显示失败原因；学生打开 `#/teacher` 被回退；已有 `#/runs/42/editor` 直接恢复编辑器。所有请求命中本地 Node 模拟 API，不能替代真实后端授权验收。
- 2026-10-01 复验：Java 后端 75 项、Harness 17 项、API 配置 Node 测试 7 项均通过；`node --check app.js`、`docker compose config --quiet`、`git diff --check` 通过。Docker Compose 运行时未启动，以上 Compose 检查只验证配置语法。

## Suggested Review Order

**路由与权限恢复**

- 先看 URL 解析、白名单回跳与身份守卫。
  [`app.js:630`](../../app.js#L630)

- 再看路由切换、服务端会话恢复与错误提示。
  [`app.js:785`](../../app.js#L785)

**工作区历史恢复**

- 检查已有运行读取、文件载入和终端会话复用。
  [`app.js:1059`](../../app.js#L1059)

- 检查画布与编辑器视图切换边界。
  [`app.js:995`](../../app.js#L995)

**页面反馈与初始化**

- 检查辅助提示的可访问状态区域与加载版本。
  [`index.html:10`](../../index.html#L10)

- 检查路由状态提示的视觉反馈样式。
  [`styles.css:514`](../../styles.css#L514)
