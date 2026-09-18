---
title: '实现登录、课程管理与实验发布前端流程'
type: 'feature'
created: '2026-09-18'
status: 'done'
context: []
---

baseline_commit: 'c5520723d32ce04c2baa424b33fa22794129fa64'

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** 当前原型直接进入实验文件库，没有登录身份、学生/教师入口、课程关系或实验发布状态，无法承载教师布置实验、学生加入课程并查看已发布实验的完整流程。

**Approach:** 在现有原生 HTML/CSS/JS 单页原型中新增登录页、角色工作台和本地状态路由。教师工作台提供课程创建、邀请码展示、实验草稿与发布；学生工作台提供邀请码入课、已加入课程和教师已发布实验入口。后端未接入前使用 localStorage 模拟身份、课程成员关系和发布状态，保留后续替换为 API 的边界。

## Boundaries & Constraints

**Always:** 登录成功后按角色进入对应工作台；未登录访问业务视图时回到登录；错误登录、错误邀请码、重复加入和发布结果必须有明确提示；退出清理当前登录状态；发布状态在教师端和学生端保持一致；原有实验库、画布和节点编辑器能力继续可用。

**Ask First:** 后端接口字段、真实鉴权方式、课程与实验的数据库主键由后端联调时再确定；当前只实现可替换的前端状态适配层。

**Never:** 不实现真实密码校验、权限安全、数据库写入、文件上传或新的实验生成算法；不删除现有三页学习实验流程。

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| STUDENT_LOGIN | `student@demo.com` / 任意非空密码 / 学生 | 进入学生课程工作台 | 空字段提示；其他账号提示登录失败 |
| TEACHER_LOGIN | `teacher@demo.com` / 任意非空密码 / 教师 | 进入教师课程工作台 | 空字段提示；其他账号提示登录失败 |
| JOIN_COURSE | 有效邀请码 `SPRING01` | 学生课程列表增加课程 | 无效邀请码或已加入显示明确错误 |
| CREATE_COURSE | 教师填写课程名称 | 新课程生成邀请码并展示 | 空名称提示 |
| PUBLISH_EXPERIMENT | 教师选择课程并发布草稿 | 状态变为已发布，学生端显示实验 | 未选课程或重复发布显示提示 |
| AUTH_GUARD | 未登录访问任意业务视图 | 回到登录页并保存目标入口 | 登录后进入对应首页 |
| LOGOUT | 点击退出 | 清理登录状态并返回登录页 | 不保留角色访问权 |

</frozen-after-approval>

## Code Map

- `index.html` -- 现有实验库、画布、节点编辑器；新增登录、学生工作台、教师工作台和课程/发布对话框结构。
- `styles.css` -- 现有三页样式；新增身份入口、课程卡片、教师发布台和状态提示的响应式样式。
- `app.js` -- 现有实验导航和创建流程；新增 localStorage 状态、登录守卫、角色路由、课程创建/加入和发布状态同步。
- `README.md` -- 补充本地前端演示账号、入口和后端替换说明。

## Tasks & Acceptance

**Execution:**

- [x] `index.html` -- 增加登录页、角色入口、学生课程页和教师课程/发布页，同时提供可访问的表单标签与状态区域。
- [x] `styles.css` -- 为新增视图建立与现有工作台一致的视觉系统，并适配窄屏。
- [x] `app.js` -- 实现本地会话、路由保护、角色切换、课程创建/邀请码加入、草稿发布和退出清理；不破坏现有实验三页事件绑定。
- [x] `README.md` -- 记录本地演示流程和预留 API 边界。

**Acceptance Criteria:**

- Given 未登录，when 打开页面或尝试进入学生/教师业务视图，then 显示登录页；登录失败显示错误；成功后按角色进入对应工作台。
- Given 学生已登录，when 输入有效邀请码，then 课程出现在已加入列表；错误邀请码和重复加入分别给出明确提示。
- Given 教师已登录，when 创建课程，then 课程卡片展示名称和邀请码；when 选择实验草稿和课程发布，then 发布状态变为已发布。
- Given 教师发布实验后，when 切换为学生身份，then 学生对应课程中可以看到该实验及已发布状态。
- Given 任意角色已登录，when 点击退出，then localStorage 会话被清理并返回登录页。

## Design Notes

身份层是轻量前端适配器：`currentUser`、`courses` 和 `publishedExperiments` 统一存储在 localStorage，视图只通过渲染函数读取状态。后续接入后端时替换登录、课程和发布动作即可，不改变页面验收路径。

## Verification

**Commands:**

- `node --check app.js` -- expected: SUCCESS
- `git diff --check` -- expected: no whitespace errors

**Manual checks:**

- 使用教师演示账号创建课程、查看邀请码、发布实验；退出后使用学生演示账号加入课程并看到已发布实验。
- 验证无效登录、无效邀请码、重复加入、空课程名和退出状态清理。

## Suggested Review Order

**页面入口与路由保护**

- 先查看三类入口结构，确认角色工作台与既有实验流程共存。
  [`index.html:10`](../../index.html#L10)

- 查看本地状态键与演示数据边界，理解后续替换 API 的位置。
  [`app.js:85`](../../app.js#L85)

- 查看统一路由守卫，确认未登录和角色不匹配时的回退行为。
  [`app.js:335`](../../app.js#L335)

**课程与实验发布交互**

- 查看登录提交校验与按角色进入工作台的逻辑。
  [`app.js:1244`](../../app.js#L1244)

- 查看学生邀请码加入、无效邀请码和重复加入提示。
  [`app.js:1272`](../../app.js#L1272)

- 查看教师创建课程及邀请码生成逻辑。
  [`app.js:1299`](../../app.js#L1299)

- 查看教师发布实验并同步学生端状态的逻辑。
  [`app.js:1325`](../../app.js#L1325)

- 查看退出时清理会话并回到登录页的逻辑。
  [`app.js:1376`](../../app.js#L1376)

**视觉与使用说明**

- 查看新增身份、课程卡片和发布状态的响应式视觉样式。
  [`styles.css:488`](../../styles.css#L488)

- 查看本地演示账号、操作链路和后端替换边界。
  [`README.md:21`](../../README.md#L21)
