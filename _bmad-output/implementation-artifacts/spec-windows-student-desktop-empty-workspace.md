---
title: 'Windows 学生桌面端与空白本地实验工作区'
type: 'feature'
created: '2026-10-19'
status: 'in-progress'
baseline_commit: 'c213781f4ddb5401cd303d78660591d60ef495c3'
context:
  - '{project-root}/app.js'
  - '{project-root}/backend/src/main/java/com/bmhs/workspace/WorkspaceService.java'
  - '{project-root}/backend/src/main/java/com/bmhs/workspace/WorkspaceRepository.java'
  - '{project-root}/database/schema.sql'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** 当前新实验运行依赖服务器 Docker，并自动写入模板源码；学生需安装 Docker，无法从空白项目亲自搭建。

**Approach:** 新增 Windows 10/11 Electron 学生端，复用现有页面，由本机助手创建空白目录并提供文件、命令、进程和主动指导能力。Electron 主进程代理 HTTPS API 与现有 HttpOnly 会话；服务端只存运行及随机本地工作区标识，不存学生电脑绝对路径。

## Boundaries & Constraints

**Always:** 启用 Electron 隔离、沙箱、`nodeIntegration=false`；IPC 仅接受打包页面及窄化校验接口。工作区限于应用数据目录，拒绝路径穿越/链接逃逸。新目录不得有 README、样例、依赖清单或源码，仅允许元数据和 Git 初始化文件。Python/Git 随安装包提供，不要求学生另装 Docker/Python/Git。文件、命令、输出和并发设上限；长进程可查、可停。DeepSeek 密钥存本机系统安全存储；只在学生主动求助并确认发送范围后读取必要代码/终端信息，排除常见凭据文件。兼容旧服务器工作区，不迁移旧数据。

**Ask First:** 改变 Windows 范围、平台认证或源码上传范围；触碰服务器、Nginx、生产库或现有 Docker 资源。

**Never:** 远程页面不得操作本机文件/命令；密码、Cookie、模型密钥不得进入渲染端、日志或平台；不自动传代码/改代码/预置源码。本阶段不做 Git push 教师检查、课程 Bug RAG、Python 教学 Skill、服务器 Docker 迁移；不清理或覆盖现有改动。

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| 新建运行 | 已登录学生启动实验 | 绑定本机空目录；无项目文件 | 失败不标记就绪，可安全重试 |
| 命令/进程 | 在运行目录执行短命令或启动长进程 | 有界状态/输出；长进程可停止 | 超时终止，不自动重放 |
| 登录/指导 | 登录或主动求助并确认发送范围 | 主进程代理 API；必要上下文发 DeepSeek | 会话/模型失败或拒绝共享时不上传并说明 |
| 越权调用 | 远程页面、穿越路径或外链 | 拒绝且不访问外部文件 | 安全记录不含敏感内容 |

</frozen-after-approval>

## Code Map

- `index.html`、`app.js`、`api-config.js`、`styles.css` -- 现有页面及 API 调用，需加桌面适配层。
- `WorkspaceService.java`、`WorkspaceRepository.java`、`WorkspaceModels.java`、`WorkspaceController.java` -- 运行/工作区合同；新桌面运行不得触发模板/Docker，服务端不存本机路径。
- `WorkspaceTemplateService.java`、`DockerWorkspaceRuntime.java`、`DockerTerminalRuntime.java` -- 旧路径保留，兼容历史运行。
- `GuidanceService.java`、`DeepSeekClient.java` -- 当前服务器代码读取和服务器密钥路径；桌面个人密钥需隔离。
- `database/schema.sql`、`database/migrations/` -- 仅允许兼容旧记录的增量迁移。
- `backend/pom.xml`、根目录构建配置 -- 当前无 Electron 工程，新增可复现 Windows 打包。
- `desktop/src/main.js`、`preload.js`、`assistant/` -- 新增 Electron 主进程、窄 IPC 与本地能力模块。

## Tasks & Acceptance

**Execution:**
- [ ] `desktop/package.json`、`desktop/src/main.js`、`desktop/src/preload.js` -- 建立打包入口、隔离窗口与来源校验 IPC。
- [ ] `desktop/src/assistant/WorkspaceManager.js`、`ProcessManager.js` -- 实现空目录、受限文件/命令、长进程生命周期、有界日志；把 Python/Git 放入 Windows 安装包。
- [ ] `desktop/src/main.js`、`desktop/src/platform/ApiSession.js` -- 限定 HTTPS API origin，由主进程代理 HttpOnly 登录会话。
- [ ] `backend/src/main/java/com/bmhs/desktop/`、`WorkspaceService.java`、`WorkspaceRepository.java`、`database/migrations/` -- 为新本地运行存随机关联 ID，旧服务器运行沿用旧路径。
- [ ] `desktop/src/assistant/GuidanceService.js`、`SecretStore.js`、`app.js`、`api-config.js` -- 保存本地密钥、征得范围确认后过滤上下文并调用 DeepSeek。
- [ ] `desktop/test/`、`backend/src/test/java/com/bmhs/desktop/`、`tests/` -- 覆盖 IPC、路径、空目录、进程、脱敏、会话和旧运行兼容。

**Acceptance Criteria:**
- Given 干净 Win10/11 无 Docker/Python/Git，when 安装、登录并启动实验，then 新建本地空目录，无预置源码。
- Given 学生运行 Python 命令并启动长进程，when 检查工作区，then 文件操作限定于运行目录且进程可停止、输出有界。
- Given 页面调用平台 API，when 登录/请求，then Cookie 和 DeepSeek 密钥只由主进程管理。
- Given 远程 IPC 或越界/链接路径，when 请求能力，then 拒绝且不访问外部文件。
- Given 学生未主动求助或未确认范围，when 编辑/保存，then 不调用模型、不上传代码；确认后只传必要脱敏上下文。
- Given 已有旧 Docker 工作区，when 新桌面运行，then 旧数据可只读查看且服务器不接收本机绝对路径。

## Design Notes

桌面运行和历史服务器工作区必须显式区分；失败时不得回退到模板/Docker。API 仅代理配置的 HTTPS origin，禁止任意 URL。Windows 子进程通过可控进程组/Job Object 管理，退出时不会遗留无法停止的后台服务。

## Verification

**Commands:**
- `node --test tests/*.test.js` -- expected: frontend/API and桌面桥接测试通过。
- `mvn -f backend/pom.xml test` -- expected: all existing and new backend compatibility tests pass.
- Windows 干净虚拟机安装验收 -- expected: 无额外环境安装，完成登录、空白目录、文件/Python 命令、长进程启停、主动求助和退出。
- `git diff --check` -- expected: no whitespace errors.

**Manual checks:** 实际打包页面检查登录和编辑器/终端/求助；验证远程页面无 IPC 权限、旧运行只读兼容。记录安装体积、启动时间、命令延迟、错误率和资源占用。
