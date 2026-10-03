---
title: '让个人实验库读取并运行真实服务端实验'
type: 'feature'
created: '2026-10-01'
status: 'in-review'
baseline_commit: '5f510e46f87dee4549de9216e501ab7cd738a048'
context:
  - '{project-root}/app.js'
  - '{project-root}/backend/src/main/java/com/bmhs/course/CourseRepository.java'
  - '{project-root}/backend/src/main/java/com/bmhs/workspace/WorkspaceRepository.java'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** 实验库展示静态样例，新建实验刷新后消失；学生也无法运行自己创建的私有实验。

**Approach:** 按当前会话用户从数据库加载本人实验，并允许学生运行自己的草稿；移除静态样例，创建、刷新和重新登录后都使用服务端数据。

## Boundaries & Constraints

**Always:** 用 `AuthenticatedUser.id` 校验 `creator_id`；个人列表与课程分配分开；课程成员校验保持不变；保留现有数据，并提供加载、空状态和错误反馈。

**Ask First:** 无。

**Never:** 不新增表/迁移，不硬删除数据，不信任前端权限，不列出他人或仅课程可见的实验，不回退到静态数据。

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|----------------------------|----------------|
| PERSONAL_LIST | 已登录用户打开实验库 | 仅返回本人未归档实验，按更新时间排序 | 未登录返回 401 |
| EMPTY_OR_ERROR | 空列表或服务不可用 | 显示空态/错误及重试入口 | 不显示演示数据 |
| OWN_DRAFT_RUN | 学生启动自己的草稿 | 后端允许创建或复用运行实例 | 他人草稿仍拒绝 |
| COURSE_RUN | 学生启动已发布课程实验 | 维持现有发布与成员校验 | 越权请求拒绝 |

</frozen-after-approval>

## Code Map

- `CourseModels.java`, `CourseRepository.java`, `CourseService.java`, `CourseController.java` -- 个人实验摘要、查询和认证接口；当前只有教师草稿列表。
- `WorkspaceRepository.java`, `WorkspaceService.java` -- 运行实例创建；当前只检查课程发布权限。
- `CourseServiceTest.java`, `WorkspaceServiceTest.java` -- 现有服务层单测，扩展所有权和角色覆盖。
- `app.js`, `index.html`, `styles.css` -- 实验库当前含静态数据，且管理控件只改本地状态。
- `deferred-work.md` -- 记录不在本工单内的持久化管理能力。

## Tasks & Acceptance

**Execution:**
- [x] `CourseModels.java`, `CourseRepository.java`, `CourseService.java`, `CourseController.java` -- 增加 `GET /api/experiments/mine`，仅按当前用户 `creator_id` 查询未归档实验，更新时间倒序；课程发布实验继续走课程授权。
- [x] `WorkspaceRepository.java` -- 允许学生运行本人创建的私有草稿，保留已发布课程实验的成员校验；拒绝他人草稿和未授权实验。
- [x] `CourseRepositoryTest.java`, `CourseServiceTest.java`, `WorkspaceRepositoryTest.java`, `WorkspaceServiceTest.java` -- 覆盖所有者过滤、角色限制、本人草稿运行、他人草稿拒绝和课程实验授权回归。
- [x] `app.js`, `index.html`, `styles.css` -- 删除静态实验示例；进入库时读取服务端，创建成功后重载；显示加载、空态、错误和重试；隐藏或明确标示尚未持久化的分组/删除控件，避免误导；打开真实实验时默认选中根节点并显示真实节点详情。
- [x] `deferred-work.md` -- 将分组持久化、归档/删除列为后续，不宣称本阶段已完成。

**Acceptance Criteria:**
- Given 两名学生各有实验，when 任一学生刷新或重新登录，then 只看到自己创建且未归档的实验。
- Given 学生启动本人草稿，when 请求创建运行实例，then 可创建或恢复工作区；换成其他学生请求则被拒绝。
- Given 学生启动课程实验，when 其为课程有效成员且实验已发布，then 原运行流程仍成功；非成员仍被拒绝。
- Given 实验列表为空或服务端失败，when 打开实验库，then 页面显示明确空态或错误/重试入口，绝不展示示例数据。
- Given 学生打开个人实验，when 画布加载完成，then 节点检查器显示真实节点名称、说明和耗时，不保留静态示例字段。

## Verification

**Commands:**
- `mvn -f backend/pom.xml test` -- all backend tests pass。
- `node --check app.js` -- no syntax errors。
- `docker compose config --quiet` -- deployment configuration remains valid。

**Manual checks:**
- 使用两个学生账号验证数据隔离、刷新恢复、本人草稿运行和课程实验授权。
- 验证空列表、后端不可用和重试；确认分组/删除控件不会暗示未实现的持久化行为。

## Verification Record (2026-10-01)

- 当前工作区浏览器访问实验库时显示服务端加载失败及“重试加载”，未回退到样例数据；本地 Node 模拟 API 对 `GET /api/experiments/mine` 返回 404。此项只验证前端错误态，未验证真实 Spring API、用户隔离或数据库持久化。
