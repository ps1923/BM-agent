---
title: '补齐远程页面 API 与会话部署链路'
type: 'bugfix'
created: '2026-10-01'
status: 'in-review'
baseline_commit: '5f510e46f87dee4549de9216e501ab7cd738a048'
context:
  - '{project-root}/app.js'
  - '{project-root}/index.html'
  - '{project-root}/compose.yaml'
  - '{project-root}/README.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** 前端默认将所有业务请求发送到 `http://127.0.0.1:8080/api`。远程学生浏览公网页面时，`127.0.0.1` 指向学生自己的设备，导致登录、课程和实验等请求失败；HTTPS 页面也不能安全调用明文 HTTP API。仅更改前端基址仍不够：服务器上的 API 只绑定 loopback，必须由同源反向代理接通，并在 HTTPS 环境启用 Secure 会话 Cookie。

**Approach:** 保留显式配置的 `window.BM_API_BASE_URL`；本地 `localhost`/`127.0.0.1:8000` 联调默认连接本机 8080 API；其他部署默认请求同源 `/api`，由反向代理转发到仅监听本机的 API 服务。补充可复用的 Nginx 代理示例、HTTPS Cookie 环境变量说明、纯函数测试和浏览器验收步骤。此工单不直接更改云服务器配置。

## Boundaries & Constraints

**Always:** 同源部署不得因 API 地址配置触发跨域或混合内容；保留显式 API 地址覆盖和现有 `credentials: 'include'`；跨源覆盖必须配置精确的 `ALLOWED_ORIGIN`，且不得承诺跨站 Lax Cookie 可用；保持 API 端口仅绑定 loopback；文档示例必须保留 `/api` 前缀并代理到 `127.0.0.1:8080`；HTTPS 生产环境须启用 `AUTH_SECURE_COOKIE=true`，并验证 Set-Cookie 带 `Secure; HttpOnly; SameSite=Lax`。

**Ask First:** 修改或重载云端 Nginx、域名、TLS、生产环境变量或线上服务配置。

**Never:** 将后端 8080 直接暴露公网；放宽认证、CORS 凭证或 Cookie 安全策略；在 URL 查询参数中接受可控 API 地址；把本地开发默认误用于远程来源。

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|----------------------------|----------------|
| LOCAL_DEV | 无覆盖，页面位于 `http://127.0.0.1:8000` 或 `http://localhost:8000` | 使用对应 loopback 主机的 `:8080/api` | 不改变请求认证选项 |
| REMOTE_HTTPS | 无覆盖，页面位于公开 HTTPS origin，代理与 Secure Cookie 已配置 | 使用同源 `/api`；登录后浏览器保存 HttpOnly/Secure Cookie，后续 `/api/me` 成功 | 代理、证书或 Cookie 配置不符时不得伪报登录成功 |
| EXPLICIT_OVERRIDE | 配置 `window.BM_API_BASE_URL` | 校验并使用覆盖地址（去除末尾斜线） | HTTPS 页面拒绝明文 HTTP 覆盖地址并显示可理解错误 |
| PROXY_MISSING | 远程页面同源 API 路径未配置代理 | 不回退到浏览器本机地址 | 保留明确的服务连接错误，不显示假数据 |

</frozen-after-approval>

## Code Map

- `api-config.js` -- 纯函数解析 API 基址，供浏览器入口和 Node 测试共用。
- `index.html` -- 确保配置脚本在 `app.js` 之前加载。
- `app.js` -- 统一读取解析后的基址，保留现有请求凭证行为。
- `tests/api-config.test.js` -- 覆盖本地、远程、覆盖值及边界输入。
- `README.md` -- 记录本地默认规则、生产 `/api` 反向代理示例、HTTPS Secure Cookie 配置和外网验收流程。
- `backend/src/test/java/com/bmhs/auth/AuthControllerTest.java` -- 验证启用 Secure 模式时会话 Cookie 属性。
- `backend/src/main/java/com/bmhs/config/WebConfig.java`, `backend/src/main/resources/application.yml`, `.env.example` -- 支持多个精确 CORS origin 并配置本地两种 loopback 页面地址。
- `compose.yaml` -- 核对 API 继续只映射至 loopback，不做网络暴露变更。

## Tasks & Acceptance

**Execution:**
- [x] `api-config.js`, `tests/api-config.test.js` -- 实现并单测基址解析，避免生产请求误发至客户端 loopback；覆盖浏览器脚本导出、查询参数和片段拒绝。
- [x] `index.html`, `app.js` -- 加载并使用解析器，保留请求凭证和显式部署覆盖。
- [x] `backend/src/test/java/com/bmhs/auth/AuthControllerTest.java` -- 覆盖 HTTPS 部署的 Secure/HttpOnly/SameSite Cookie 属性。
- [x] `backend/src/main/java/com/bmhs/config/WebConfig.java`, `backend/src/main/resources/application.yml`, `.env.example` -- 允许本地两个精确 origin，不使用 CORS 通配符并保留生产显式配置；拒绝带路径、凭据、查询、片段、通配符和非规范化 Origin 的配置。
- [x] `README.md`, `compose.yaml` -- 写清并核验 Nginx `/api` 代理、`ALLOWED_ORIGIN`、HTTPS Secure Cookie 契约；确认 API 端口仍为 loopback 绑定。只提供示例，不更改云端配置。
- [ ] 浏览器回归 -- 在本地模式、远程 HTTPS 同源代理模拟环境和显式覆盖环境验证请求目标、登录、Cookie 和 `/api/me` 会话恢复。

**Acceptance Criteria:**
- Given 页面运行在本地 `localhost:8000` 且没有覆盖值，when 请求任一 API，then 请求目标为本机 API `:8080/api`。
- Given 页面运行在远程 HTTPS origin 且没有覆盖值，when 请求任一 API，then 请求目标为同源 `/api`，不得包含 `localhost`、`127.0.0.1` 或不安全的 `http://`。
- Given HTTPS 页面和生产代理，when 用户登录并随后请求 `/api/me`，then 会话可恢复且 Set-Cookie 包含 `Secure`、`HttpOnly` 和 `SameSite=Lax`。
- Given 部署配置显式提供安全有效的 API 基址，when 页面初始化，then 覆盖值生效且既有 Cookie 凭证选项保持不变；HTTPS 页面不得接受明文 HTTP 覆盖值。
- Given Docker Compose 配置，when 检查 API 端口映射，then 8080 仍仅绑定服务器 loopback。
- Given 生产部署文档，when 按示例配置反向代理，then `/api/...` 原样转发到后端且不移除 `/api` 前缀。

## Spec Change Log

- 初稿复核后扩展验收范围：前端基址正确并不代表远程登录可用；加入反向代理、HTTPS Secure Cookie 和会话恢复要求，避免仅在本地模拟通过却无法外网使用。云端 Nginx 仍须单独批准后才能修改。
- 独立边界审查发现大小写协议/域名及显式默认端口虽是等价 URL，却可能与浏览器序列化的 `Origin` 不同，导致凭证 CORS 校验失败；现改为启动时拒绝非规范写法，并增加大小写、默认端口及非默认端口测试。

## Verification

**Commands:**
- `node --test tests/api-config.test.js` -- local/remote/override and insecure-HTTPS override cases pass.
- `node --check app.js` -- no frontend syntax errors.
- `docker compose config --quiet` -- deployment configuration remains valid.
- `mvn -f backend/pom.xml test` -- backend suite passes (77 tests), including canonical CORS-origin edge cases.
- `.venv/Scripts/python.exe -m pytest harnesses -q` -- Harness suite passes (17 tests).

**Manual checks:**
- The local editor page renders, but the available `127.0.0.1:8080` service is a Node mock: `/api/me` returns a fixed QA identity and `/api/experiments/mine` returns 404. Thus this is not proof of real login, authenticated session restoration, or experiment loading. Remote HTTPS proxy simulation, explicit override in a browser, cookie attributes, and authenticated `/api/me` remain unverified.
- Browser interaction check: opening the editor deep link and selecting “返回画布” updates the URL to the experiment canvas and renders its node inspector. This local flow is backed by the QA mock and does not satisfy the remote HTTPS/session acceptance criterion.
- Read-only public probe on 2026-10-01: `http://47.109.176.135/api/me` returned HTTP 404; HTTPS to the same IP failed during TLS handshake. This confirms the public session path is not currently verifiable by IP and does not establish the deployed Nginx/TLS configuration.
- The public cloud environment is not declared accepted; no cloud configuration was changed. Production proxy/Cookie settings and a real browser session test require separate authorization and verification.

## Suggested Review Order

**API target selection and session request path**

- Resolve local versus remote API destinations and reject insecure HTTPS overrides.
  [`api-config.js:11`](../../api-config.js#L11)
- Require the shared resolver at startup and preserve cookie credentials on API calls.
  [`app.js:117`](../../app.js#L117)
- Load the resolver before application initialization.
  [`index.html:235`](../../index.html#L235)

**Credentialed CORS and production routing**

- Reject non-canonical origins that would not match the browser's serialized Origin.
  [`WebConfig.java:39`](../../backend/src/main/java/com/bmhs/config/WebConfig.java#L39)
- Keep API ports loopback-only and preserve `/api` through the proxy example.
  [`README.md:123`](../../README.md#L123)
- Verify cookie attributes and CORS-origin edge cases.
  [`WebConfigTest.java:37`](../../backend/src/test/java/com/bmhs/config/WebConfigTest.java#L37)

Production HTTPS login, cookie receipt, and real `/api/me` restoration remain pending; this spec stays `in-review` until those acceptance checks are actually completed.
