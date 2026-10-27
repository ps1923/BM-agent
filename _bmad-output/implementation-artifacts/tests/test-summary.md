# Git 提交与 AI 节点验收端到端测试记录

日期：2026-10-27
范围：BM-HS 登录 → 学生实验运行 → Gitea 仓库 → HTTP Git push → AI 节点验收 → 教师查看提交与差异

## 测试资产

- 自动化测试：`tests/git-acceptance-live-e2e.test.js`（Node.js 内置 `node:test`，通过 `BMHS_E2E_*` 环境变量启用）。
- 隔离实验：实验 ID `10`，名称以“全链路测试”目标创建，已发布到测试课程。
- 学生运行：run ID `11`，Git 模式。
- 测试学生原有实验运行未被复用或修改。

## 最终结果

**端到端测试通过。** 首次运行发现并修复了两个真实阻断点，修复后自动化测试完整跑通。

| 环节 | 结果 | 证据 |
|---|---|---|
| 学生、教师登录 | 通过 | 两种角色均成功取得会话 |
| 隔离实验生成与发布 | 通过 | 实验 ID `10`，包含 4 个节点，发布到测试课程 |
| 学生 Git 运行实例创建 | 通过 | run ID `11`，模式为 `git` |
| Gitea 私有仓库与 webhook | 通过 | 私有仓库和 push webhook 创建/修复成功 |
| 学生凭据签发 | 通过 | 已改为专属账号密码认证签发限仓库读写范围的 Token，并加密保存；失败重试可恢复 |
| 本地 Git commit / HTTP push | 通过 | Gitea `git-receive-pack` 返回 `200 OK` |
| DeepSeek 节点验收 | 完成 | submission ID `3`，commit `8898d21f9c7389943dd9c7279e1a8ff8cb0f1ad5`，状态 `completed` |
| 教师查看 commit 与差异 | 通过 | 教师端可读取验收结果及 `README.md`、`main.py` 差异 |

逐节点结果：节点 `90` 为 `meets`，提供 2 条可核验证据；节点 `91` 为 `unable_to_verify`，模型未给出能与源码行匹配的证据，因此系统按规则降级，不能当作通过。该节点建议补充非数字输入的实际运行记录。AI 本轮只做静态检查，没有运行项目。

## 已修复的问题

1. Gitea 28.2.0 的用户 Token 接口要求用户密码认证。原实现使用管理员 Token 加 `Sudo`，返回 401。现对专属仓库账号生成/重置随机初始密码，以 Basic 认证签发仓库读写范围 Token，随后加密存储；未向学生暴露管理员凭据。
2. DeepSeek 响应结构曾因格式或无法核验的证据使整份验收失败。代码验收现启用 DeepSeek JSON 输出模式；无法匹配源码的证据会被丢弃，若“符合”结论没有可验证证据则降为“无法判断”，保留其余有效节点结果。
3. 学生页面先前只接受 HTTPS 克隆地址，无法展示服务器 HTTP Gitea 地址。现允许 HTTPS 地址，以及与当前 BM-HS 页面同主机的 HTTP 演示地址；仍拒绝跨主机 HTTP 和嵌入式账号密码。

后端验收修复部署在 `/opt/bm-agent/releases/20261027-gitea-acceptance-json-fix`，当前静态页面发布为 `/opt/bm-agent/releases/20261027-http-git-public-app-fix`。API 健康检查通过，保留先前发布供回滚。

本地学生仓库位于 `D:\WORK\BM-HS-agent\_e2e-student-experiment`，分支 `main`，当前 commit 与上表一致；远程为该实验运行专属的 Gitea 私有仓库。未推送项目 GitHub 主仓库。

服务器保留隔离测试实验 `10`、运行 `11`、提交及对应 Gitea 仓库，均未删除。

## 测试命令

在本地设置 `BMHS_E2E_BASE_URL`、`BMHS_E2E_EXPERIMENT_ID`、`BMHS_E2E_STUDENT_EMAIL`、`BMHS_E2E_STUDENT_PASSWORD`、`BMHS_E2E_TEACHER_EMAIL`、`BMHS_E2E_TEACHER_PASSWORD` 后运行：

```text
node --test tests/git-acceptance-live-e2e.test.js
```

测试不会打印账号密码、仓库凭据或会话 Cookie；Git 凭据只放在本次进程环境中。设置 `BMHS_E2E_WORKSPACE` 时，测试会在该目录下新建独立子目录并保留学生仓库供人工检查，不会覆盖目录中已有文件；未设置时使用临时目录并在结束时删除。
