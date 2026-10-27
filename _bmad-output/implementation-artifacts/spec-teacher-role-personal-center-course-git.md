---
title: '教师课程管理与师生个人中心'
type: 'feature'
created: '2026-10-23'
status: 'in-progress'
baseline_commit: 'b01284fe23e6c657980897e71bd0e2a274792323'
context:
  - '{project-root}/backend/src/main/java/com/bmhs/auth/AuthService.java'
  - '{project-root}/backend/src/main/java/com/bmhs/course/CourseService.java'
  - '{project-root}/backend/src/main/java/com/bmhs/git/GitAcceptanceService.java'
---

<frozen-after-approval reason="explicitly approved implementation plan">

## Intent

**Problem:** 独立管理员角色拥有全站 Gitea 密钥和个人实验审核入口，学生与教师没有通用个人中心；课程也没有可管理的 Git 仓库地址。

**Approach:** 移除管理员角色及独立页面，将现有管理员迁移为教师；教师只管理自己负责课程，在课程内配置一个仓库地址、管理成员与实验；学生和教师均可编辑显示名称及 Git commit 用户名/邮箱。全站 Gitea 服务凭据继续由服务器配置管理，不开放给教师或学生。

## Boundaries & Constraints

**Always:** 服务端按当前登录用户校验资料写入；课程操作检查 `courses.teacher_id`；仓库 URL 仅允许平台配置的 Gitea 公网主机及协议，不执行用户 URL 探测；旧管理员变为教师但不得获得跨课程权限；增量迁移保留全部业务记录；保留现有每个学生实验运行实例独立仓库的隔离设计。

**Ask First:** 将全站 Gitea 密钥重新开放给教师、允许任意 Git 主机、或改变每个运行实例独立仓库的隔离模型。

**Never:** 删除课程、账号、Git 设置、提交或审核记录；将 Gitea 管理令牌写入课程表、用户资料或浏览器；保留任何前端/后端管理员角色入口。

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| 更新个人资料 | 已登录学生或教师提交显示名及 Git author name/email | 只更新当前用户并返回非敏感资料 | 验证失败返回字段错误 |
| 越权资料更新 | 请求包含其他 userId 或目标用户字段 | 不支持指定用户，当前会话用户不变 | 400/403 |
| 设置课程仓库 | 课程负责人提交允许的 Gitea 仓库 URL | 保存一个课程地址并可在课程管理读取 | URL 无效或非负责人时拒绝 |
| 跨课程读写 | 教师访问他人课程仓库设置 | 不返回仓库信息且不修改数据 | 403 |
| 角色迁移 | users.role 为 admin | 迁移后为 teacher；其他数据不变 | 迁移可重跑，不覆盖非 admin 角色 |

</frozen-after-approval>

## Code Map

- `index.html`, `app.js`, `styles.css` -- 删除管理员工作台/个人中心；为学生和教师增加个人中心；教师课程卡片提供仓库设置入口。
- `backend/src/main/java/com/bmhs/auth/` -- 个人资料 API、模型、服务和当前用户持久化。
- `backend/src/main/java/com/bmhs/course/` -- 课程仓库地址读写及负责人权限检查。
- `backend/src/main/java/com/bmhs/teacher/` -- 收敛个人实验审核管理为课程教师可见范围，保持既有课程教师审核权限。
- `backend/src/main/java/com/bmhs/git/` -- 验证课程仓库地址并限制于服务器配置的 Gitea 主机；保持运行实例仓库生成和全站凭据服务端管理。
- `database/schema.sql`, `database/migrations/`, `database/verify.sql` -- 新字段、角色约束及兼容迁移。
- `backend/src/test/java/`, `tests/` -- 角色、越权、资料、课程仓库、迁移和 UI 回归。

## Tasks & Acceptance

**Execution:**
- [ ] `database/schema.sql`, `database/migrations/`, `database/verify.sql` -- 增量添加个人 Git author 字段和课程仓库 URL，移除 admin 角色约束并将存量 admin 更新为 teacher。
- [ ] `backend/src/main/java/com/bmhs/auth/` -- 增加 `GET/PUT /api/me/profile`，只读写当前用户资料。
- [ ] `backend/src/main/java/com/bmhs/course/` -- 增加 `GET/PUT /api/courses/{courseId}/git-repository`，只允许课程负责人读取/设置；仓库 URL 必须属于服务端 Gitea 公网主机。
- [ ] `backend/src/main/java/com/bmhs/teacher/` -- 将原管理员个人实验审核教师管理改为教师可操作且只列出与其负责课程成员相关的实验；拒绝跨课程或无归属目标。
- [ ] `index.html`, `app.js`, `styles.css` -- 删除管理员路由、组件、登录角色分支与全站密钥表单；统一学生/教师个人中心并加入资料表单、课程仓库配置交互。
- [ ] 相关 Java/Node 测试 -- 更新角色夹具和旧管理员 UI 断言，新增权限、验证、迁移和交互覆盖。

**Acceptance Criteria:**
- Given 迁移前存在 admin 用户，when 增量迁移后登录，then 其角色为 teacher 且所有原有数据仍在。
- Given 学生或教师更新个人中心，when 刷新后重新读取，then 仅自身 display name 与 Git commit identity 发生更新。
- Given 教师设置所属课程仓库 URL，when 课程页面重新读取，then 显示该地址且其他课程教师无法读取或修改。
- Given 浏览器尝试访问旧管理员路由或全站 Gitea 密钥 API，when 用户是教师或学生，then 全站密钥不会通过页面提供，API 拒绝访问。
- Given 课程内学生运行新建 Git run，when 获取仓库，then 仍创建该 run 独立仓库，不将全班共享到一个可互相覆盖的仓库。

## Design Notes

课程级 URL 作为课程 Git 仓库的管理元数据，不改变现有隔离模型：实际代码验收仍使用每名学生每个 run 独立 Gitea 仓库。个人 Git author identity 仅用于展示/后续 Git author 配置，不用于认证。

## Verification

**Commands:**
- `mvn -f backend/pom.xml test` -- 后端全量测试通过。
- `node --check app.js; node --test tests/*.test.js` -- 前端脚本及 UI 测试通过。
- `node --test tests/database-migrations.test.js` -- 增量迁移静态与重跑安全测试通过。
- 在浏览器中以学生和教师分别验证个人中心、课程仓库设置及横向越权拒绝。
