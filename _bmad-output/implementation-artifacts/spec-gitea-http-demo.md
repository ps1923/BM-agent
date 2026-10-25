---
title: '演示环境支持 HTTP Gitea 提交与验收'
type: 'bugfix'
created: '2026-10-25'
status: 'in-progress'
baseline_commit: 'b01284fe23e6c657980897e71bd0e2a274792323'
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** 当前 Gitea 集成要求公网克隆地址和 Webhook 接收地址使用 HTTPS，导致没有证书的个人演示服务器无法创建学生仓库或接收提交事件。

**Approach:** 允许个人演示部署使用 HTTP 公网地址，保留地址格式校验、内部 API 网络限制、Webhook 签名校验和凭据加密；部署配置和使用说明明确仅适用于可信、临时演示环境。

## Boundaries & Constraints

**Always:** 内部 Gitea API 继续限制为 loopback 或隔离网络中的 `gitea` 主机；Webhook 必须验证至少 32 字节密钥签名；不得把密钥写入仓库、日志或前端；只提交本次 HTTP 支持所需文件，保留其他工作区改动；部署前确认服务器现有服务和数据位置。

**Ask First:** 若需要改动现有数据库数据、删除/重建 Gitea 数据卷、改变公网端口或停用其他服务，必须先询问。

**Never:** 不要求或配置 TLS；不公开 Gitea 管理端口；不将 HTTP 描述为安全传输；不提交无关的现有未提交文件；不把学生仓库或历史数据作为清理对象。

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| HTTP demo endpoint | Valid HTTP Gitea and BM-HS webhook URLs, complete secrets | Repository provisioning and signed webhook processing are allowed | Invalid host, embedded credentials, missing secret, or bad signature remains rejected |
| Invalid URL | FTP, malformed URL, URL with userinfo, or unsupported host | No credentials or repository URL issued | Existing safe configuration error |
| HTTPS deployment | Existing valid HTTPS URLs | Continues to work without behavior regression | Existing validation retained |

</frozen-after-approval>

## Code Map

- `backend/src/main/java/com/bmhs/git/GiteaClient.java` -- public URL protocol validation currently requires HTTPS; internal API restrictions must remain.
- `backend/src/main/java/com/bmhs/git/GitConfig.java` -- resolves server environment/database settings and determines whether required values exist.
- `backend/src/test/java/com/bmhs/git/` -- Gitea configuration and acceptance regression tests.
- `compose.yaml` and `README.md` -- deployment URLs and operator guidance; secrets stay outside tracked files.

## Tasks & Acceptance

**Execution:**
- [x] `GiteaClient.java` and focused tests -- permit HTTP/HTTPS only for public demo URLs while retaining URL, host, signature, and internal API protections.
- [x] `README.md` and deployment configuration examples -- document HTTP-only demo setup and warn that Git credentials traverse the network without transport encryption.
- [ ] Deployment process -- configure and start Gitea on the server without replacing existing persistent data, then verify health and a real push/webhook/AI acceptance path.

**Acceptance Criteria:**
- Given a valid HTTP demo URL and valid server secrets, when a student provisions a repository and pushes a commit, then the signed webhook is accepted and AI acceptance can run.
- Given malformed URLs, invalid signatures, or missing secrets, when a request is made, then it is rejected and no secret is returned.
- Given an HTTPS configuration, when the same flow is used, then it remains supported.
- Given other uncommitted workspace files, when this change is committed, then those files remain untouched and are excluded.

## Spec Change Log

## Verification

**Commands:**
- `mvn -f backend/pom.xml test` -- expected: all backend tests pass.
- `docker compose config --quiet` -- expected: deployment configuration validates without printing secrets.
- Real server check -- expected: Gitea reachable at the configured HTTP URL; a test push reaches the signed webhook and records an AI acceptance result.
