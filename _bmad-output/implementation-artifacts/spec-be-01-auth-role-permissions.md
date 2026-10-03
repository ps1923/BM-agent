---
title: 'BE-01 用户认证与角色权限'
type: 'feature'
created: '2026-09-18'
status: 'done'
baseline_commit: 'c5520723d32ce04c2baa424b33fa22794129fa64'
context:
  - '{project-root}/README.md'
  - '{project-root}/database/schema.sql'
  - '{project-root}/backend/src/main/java/com/bmhs/experimentcreation/ExperimentCreationController.java'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** 当前接口依赖演示用 `X-User-Id`/固定用户，不能区分真实学生和教师，也没有登录会话和密码校验，业务接口无法安全地按用户隔离。

**Approach:** 增加基于数据库的账号密码认证和不透明会话 Cookie。后端从 HttpOnly Cookie 解析当前用户，统一提供认证状态与角色守卫；现有实验创建接口改为使用当前会话用户，并彻底删除演示身份配置和请求头逻辑。

## Boundaries & Constraints

**Always:** 密码只保存 BCrypt 哈希；会话 Cookie 不保存用户 ID 或密码；服务端查询会话后才能得到用户；登录、退出、当前用户接口返回明确的 401/403；会话过期、禁用用户和错误密码都不能泄漏账号存在性；跨域请求启用 credentials，Cookie 默认 HttpOnly、SameSite=Lax，生产环境可配置 Secure。

**Ask First:** 无。BE-01 不增加公开注册和密码找回；账号由数据库初始化或后续管理员流程创建。

**Never:** 不再接受 `X-User-Id`；不回退到固定 demo 用户；不把密码、原始会话令牌写入日志或 JSON；不把角色放在客户端可修改的字段中。

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|----------------------------|----------------|
| LOGIN | 有效 email/password，用户 active | 200，设置 HttpOnly 会话 Cookie，返回用户摘要 | 错误凭据统一 401 |
| AUTH_REQUIRED | 无 Cookie 或会话已过期访问业务接口 | 不执行业务逻辑 | 401 `AUTHENTICATION_REQUIRED` |
| ROLE_FORBIDDEN | student 请求 teacher-only 接口 | 不执行教师逻辑 | 403 `ROLE_FORBIDDEN` |
| LOGOUT | 有效或已失效 Cookie | 清除 Cookie，并使服务端会话失效 | 重复退出仍返回成功 |
| DISABLED_USER | 密码正确但用户 disabled | 不建立会话 | 401，不暴露账号状态 |

</frozen-after-approval>

## Code Map

- `backend/pom.xml` -- 增加密码哈希实现依赖与认证测试依赖。
- `database/schema.sql` -- 为 users 增加 password_hash，并新增 user_sessions 会话表。
- `database/migrations/20260918_add_auth.sql` -- 对已有 BM-sql 实例执行幂等认证表结构迁移。
- `backend/src/main/java/com/bmhs/auth/*` -- 用户、会话、登录退出、当前用户和角色守卫。
- `backend/src/main/java/com/bmhs/config/WebConfig.java` -- 允许 credentials，移除 X-User-Id 请求头。
- `backend/src/main/java/com/bmhs/experimentcreation/ExperimentCreationController.java` -- 从当前认证用户取 userId，删除 demo 身份分支。
- `backend/src/main/resources/application.yml` -- 配置 Cookie 名称、有效期、Secure 等非秘密参数。

## Tasks & Acceptance

**Execution:**
- [x] `database/schema.sql`, `database/migrations/20260918_add_auth.sql` -- 增加 `users.password_hash`、会话哈希/过期/撤销字段、索引和外键，并保持迁移可重复执行。
- [x] `backend/pom.xml`, `backend/src/main/java/com/bmhs/auth/*` -- 实现 BCrypt 校验、随机不透明令牌、会话持久化、登录/退出/当前用户接口及认证过滤器。
- [x] `backend/src/main/java/com/bmhs/auth/*` -- 实现基于服务端角色的守卫，并提供一个教师专用用户接口用于验证 student 被拒绝。
- [x] `backend/src/main/java/com/bmhs/experimentcreation/ExperimentCreationController.java`, `backend/src/main/java/com/bmhs/config/WebConfig.java`, `backend/src/main/resources/application.yml` -- 删除 demo 用户和 `X-User-Id`，业务接口统一要求 Cookie 会话。
- [x] `backend/src/test/java/com/bmhs/auth/*`, `backend/src/main/java/com/bmhs/experimentcreation/*` -- 覆盖登录、退出、Cookie、401/403、禁用用户、密码哈希和现有会话用户隔离。

**Acceptance Criteria:**
- Given active student/teacher accounts, when they submit valid credentials, then each can log in and later log out, and the browser receives only an HttpOnly session Cookie.
- Given no valid session, when a user calls any business endpoint, then the endpoint returns 401 and does not access the experiment service.
- Given a student session, when the student calls the teacher-only endpoint, then the endpoint returns 403; a teacher session succeeds.
- Given the database, when inspecting user records, then no plaintext password exists and login verifies only the stored hash.
- Given a request containing `X-User-Id`, when it calls a business endpoint, then the header is ignored/rejected and cannot change the authenticated user.

## Design Notes

Store only SHA-256 of a random 32-byte session token in `user_sessions`; the raw token exists only in the Cookie and is rotated on login. Store BCrypt password hashes in `users.password_hash`. The current request user is server-derived and passed to controllers through a request attribute or small argument resolver.

## Verification

**Commands:**
- `mvn -f backend/pom.xml test` -- expected: all Java tests pass.
- `node --check app.js` -- expected: existing frontend syntax remains valid.
- `mysql --user=root --password --execute="SOURCE database/migrations/20260918_add_auth.sql"` -- expected: migration succeeds on BM-sql.

## Suggested Review Order

**认证与会话边界**

- BCrypt 校验、令牌生成与无账号信息泄漏的统一错误处理。
  [`AuthService.java:41`](../../backend/src/main/java/com/bmhs/auth/AuthService.java#L41)

- 登录、退出和 HttpOnly Cookie 的生命周期配置。
  [`AuthController.java:42`](../../backend/src/main/java/com/bmhs/auth/AuthController.java#L42)

- Cookie 会话如何转换为服务端当前用户，并跳过非 API 静态资源。
  [`SessionAuthenticationFilter.java:27`](../../backend/src/main/java/com/bmhs/auth/SessionAuthenticationFilter.java#L27)

**权限与业务接入**

- 401/403 统一守卫，以及教师角色校验入口。
  [`AuthContext.java:14`](../../backend/src/main/java/com/bmhs/auth/AuthContext.java#L14)

- 实验创建接口改为只接收服务端解析出的用户身份。
  [`ExperimentCreationController.java:25`](../../backend/src/main/java/com/bmhs/experimentcreation/ExperimentCreationController.java#L25)

**数据库与验证**

- 用户密码哈希和会话令牌表结构。
  [`schema.sql:10`](../../database/schema.sql#L10)

- 现有 BM-sql 实例的幂等认证迁移。
  [`20260918_add_auth.sql:1`](../../database/migrations/20260918_add_auth.sql#L1)

- 认证、角色、退出和 Cookie 行为测试。
  [`AuthServiceTest.java:18`](../../backend/src/test/java/com/bmhs/auth/AuthServiceTest.java#L18)

