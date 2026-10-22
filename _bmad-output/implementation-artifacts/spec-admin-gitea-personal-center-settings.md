---
title: '管理员个人中心的 Git 仓库配置'
type: 'feature'
created: '2026-10-22'
status: 'done'
baseline_commit: '77c19d83645c5fd0ea51223c548d6e6886b7a665'
context:
  - '{project-root}/backend/src/main/java/com/bmhs/git/GitConfig.java'
  - '{project-root}/backend/src/main/java/com/bmhs/git/GitCredentialCipher.java'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** 管理员目前没有页面配置 Gitea，Git 仓库连接信息只能通过服务器环境变量维护；即使新增表单，若运行时仍只读取环境变量，保存也不会让 Git 流程生效。

**Approach:** 新建管理员可访问的个人中心页面，提供全站 Git/Gitea 连接配置表单，保存到云服务器 MySQL 并供后端 Git 流程运行时读取。学生和教师不得查看或修改该共享配置。

## Boundaries & Constraints

**Always:** 后端强制管理员授权；API 不返回任何密钥明文；Gitea 管理令牌和 Webhook 密钥以现有 AES-GCM 思路加密后入库，加密主密钥只留在服务器环境变量；表单秘密字段留空代表保留现有值；保存后配置即时生效；公网克隆地址与 Webhook 地址必须 HTTPS，Gitea 内部 API 仅允许 loopback 或受限的 `gitea` 服务名；输入 URL 要校验，避免 SSRF；使用增量迁移并保留既有配置和数据。

**Ask First:** 需要放宽公网 HTTPS 要求、把加密主密钥存入数据库、或允许非管理员修改全站配置时，先询问用户。

**Never:** 不向浏览器返回明文密钥；不把任意 URL 交给服务器探测；不记录令牌/密钥到日志；不让学生或教师修改全站 Gitea 配置；不在本任务中部署 Gitea、签发证书或改动线上服务器。

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| 保存配置 | 管理员提交合法 URL 与配置；秘密字段可留空 | 配置持久化至单例设置记录；秘密留空时保留已有密文；Git 服务读取新值 | 表单显示保存结果，不回显密钥 |
| 未授权读取或保存 | 学生/教师调用管理员接口 | 拒绝请求，不返回配置内容 | 统一 403 |
| 无效或不安全地址 | 公网地址为 HTTP、内网 API 主机不在允许列表，或 URL 含凭据 | 拒绝保存，不发起网络请求 | 返回可理解的字段错误 |
| 秘密轮换/清除 | 管理员替换密钥或显式勾选清除 | 新密钥加密保存；清除需明确操作 | 加密主密钥无效时拒绝写入并保留旧配置 |

</frozen-after-approval>

## Code Map

- `index.html` -- 管理员工作台及页面入口；新增个人中心配置视图。
- `app.js` -- 哈希路由、管理员页面渲染和表单交互。
- `styles.css` -- 页面布局、表单状态和窄屏适配。
- `backend/src/main/java/com/bmhs/git/GitConfig.java` -- 当前环境变量配置入口，需支持数据库运行时配置及兼容回退。
- `backend/src/main/java/com/bmhs/git/GitCredentialCipher.java` -- 现有 AES-GCM 加密参考；加密主密钥保持环境配置。
- `backend/src/main/java/com/bmhs/git/GiteaClient.java` -- 使用 Gitea 地址和管理令牌，保存后需读取最新设置。
- `backend/src/main/java/com/bmhs/git/GitAcceptanceService.java` -- 使用 Webhook 密钥与回调配置。
- `database/schema.sql`、`database/migrations/` -- 单例全站 Git 设置表与增量迁移。
- `backend/src/test/java/com/bmhs/git/`、`tests/` -- 权限、密钥保护、配置校验、迁移和 UI 回归测试。

## Tasks & Acceptance

**Execution:**
- [x] `index.html`, `app.js`, `styles.css` -- 新建管理员个人中心入口和 Git/Gitea 表单；显示连接状态、已配置提示和字段错误，不回显秘密。
- [x] `backend/src/main/java/com/bmhs/git/` -- 增加管理员专用读取/保存接口及运行时配置提供者；拒绝非管理员，并校验 URL、密钥长度和输入范围。
- [x] `database/schema.sql`, `database/migrations/` -- 增量添加单例设置表，密钥列只存密文，保留更新人和时间。
- [x] `backend/src/test/java/com/bmhs/git/`, `tests/` -- 覆盖权限拒绝、密钥不回显、留空保留、显式清除、加密往返、URL 安全校验、配置即时生效和页面交互。

**Acceptance Criteria:**
- Given 管理员打开个人中心，when 保存合法 Gitea 配置，then 配置写入 MySQL 且 Git 仓库服务读取到新值，无需重启应用。
- Given 学生或教师请求配置 API，when 读取或保存，then 服务端返回 403 且不泄露配置。
- Given 管理员读取配置，when 响应成功，then 只返回地址、启用状态和秘密是否已配置，不包含密钥或密文。
- Given 公网 clone/Webhook URL 使用 HTTP 或内部 API 指向未允许主机，when 保存，then 拒绝且不发起服务器端网络请求。
- Given 现有 Git 环境变量已配置但数据库尚无设置记录，when Git 功能运行，then 旧环境配置仍可作为兼容回退；保存数据库配置后数据库值优先。

## Design Notes

这是全站共享的服务器连接配置，不是学生个人 API Key。AES-GCM 主密钥只从服务器环境变量读取；数据库中的管理员令牌和 Webhook 密钥加密存放。留空与清除分开表达，避免管理员误清空仍在使用的凭据。

**Review limitation:** 保存时不探测用户提供的 URL，并拒绝常见私网/保留 IP 字面量和保留域名；但未解析外部域名，无法排除 DNS 指向私网或后续 DNS rebinding。部署前必须限制 Gitea 容器的 Webhook 出站目标，详见 `deferred-work.md`。

## Verification

**Commands:**
- `mvn -f backend/pom.xml test` -- 既有与新增 Java 测试全部通过。
- `node --check app.js; node --test tests/*.test.js` -- 前端脚本及 UI/API 配置测试通过。
- `node --test tests/database-migrations.test.js` -- 新迁移可重复执行且保留历史结构。

**Manual checks:** 管理员实测读取/保存/密钥轮换；学生和教师无法进入配置页或调用接口；窄屏可操作；浏览器不出现明文密钥；线上部署不在本任务范围内。

## Spec Change Log

## Suggested Review Order

**配置安全与运行时生效**

- 后端在单一服务中集中完成管理员授权、输入校验、加密和持久化。
  [`GitAdminSettingsService.java:40`](../../backend/src/main/java/com/bmhs/git/GitAdminSettingsService.java#L40)
- 控制器仅暴露受认证保护的读取与保存入口，并禁止缓存配置响应。
  [`GitAdminSettingsController.java:14`](../../backend/src/main/java/com/bmhs/git/GitAdminSettingsController.java#L14)
- Git 运行时优先读取数据库快照，没有记录时兼容环境变量。
  [`GitConfig.java:56`](../../backend/src/main/java/com/bmhs/git/GitConfig.java#L56)
- 令牌与 Webhook 密钥通过 AES-GCM 加密，主密钥仍由环境变量提供。
  [`GitCredentialCipher.java:30`](../../backend/src/main/java/com/bmhs/git/GitCredentialCipher.java#L30)

**持久化结构**

- 单例表只保存密文、修改人和时间戳，增量迁移不删除旧数据。
  [`20261022_add_git_global_settings.sql:5`](../../database/migrations/20261022_add_git_global_settings.sql#L5)
- Fresh-schema 定义与迁移保持一致。
  [`schema.sql:768`](../../database/schema.sql#L768)

**管理员页面交互**

- 个人中心集中展示连接状态、配置表单及密钥保留/清除操作。
  [`index.html:83`](../../index.html#L83)
- 表单通过管理员 API 保存，密钥字段不回显并支持重新读取。
  [`app.js:444`](../../app.js#L444)
- 个人中心路由在客户端要求管理员角色，服务端仍独立强制鉴权。
  [`app.js:1823`](../../app.js#L1823)

**验证与回归**

- 服务测试覆盖角色隔离、密钥加密、留空保留、显式清除和 URL 校验。
  [`GitAdminSettingsServiceTest.java:17`](../../backend/src/test/java/com/bmhs/git/GitAdminSettingsServiceTest.java#L17)
- 页面与迁移的静态回归测试防止密钥回显及破坏性 DDL。
  [`admin-gitea-settings.test.js:7`](../../tests/admin-gitea-settings.test.js#L7)
