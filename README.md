# 学习画布

这是一个让学生通过三段式向导创建树状学习实验的原型：先讨论实验方向，再填写周期与难度，最后预览并确认实验树。确认前不会写入正式实验；确认后由后端一次性创建阶段、节点、任务与连线。

- 点阵画布与可拖动节点
- 节点显示序号、绿色状态点和学习介绍
- 点击节点后从右侧打开用时、完成度、文件位置和名称
- 底部保留「开始攻克节点」主按钮样式
- 点击「开始攻克节点」进入黑底编辑器布局：任务、代码、智能体、终端与顶部工作区栏
- 首屏以文件夹方式组织实验，点击实验条目进入对应画布，画布可返回文件库
- 最近打开区域置于顶部，并按最近打开时间动态置顶；文件夹内实验按最近操作时间排序
- 文件库右侧「＋」打开三段式 Agent 创建向导：方向对话、整数周期、五档节点难度
- 只有第一段围绕学习方向进行对话，后两段只提供周期输入和难度选择
- 需求 Harness 只读取本会话的方向对话，只做事实提取、追问和需求确认
- 规划 Harness 只接收带版本和哈希的 `ConfirmedIntent`、周期及难度，只生成实验树
- 两个 Harness 是独立 Python MCP Streamable HTTP 服务，使用不同进程、端口、提示词和唯一工具
- 确认后由 Spring Boot 校验并布局实验树，再通过 MySQL 事务创建阶段、节点、任务与连线
- 实验条目支持确认删除；「管理分组」支持新建、重命名，以及删除空分组
- 「服软」、编辑器聊天和终端输入仍为前端演示，暂不连接后端

## 前端角色与课程演示

当前浏览器原型已包含登录、课程和实验发布入口，FE-01/02/03 使用 `localStorage` 模拟状态，便于后端接口完成前先验收页面流程：

- 学生：`student@demo.com`，密码填写任意非空内容
- 教师：`teacher@demo.com`，密码填写任意非空内容
- 教师可创建课程并获得邀请码；学生可使用邀请码加入课程
- 教师选择实验草稿发布到课程后，学生端会同步显示「已发布」实验
- 退出会清理当前浏览器登录状态；清理演示数据可在浏览器控制台执行 `localStorage.clear()`

前端状态适配集中在 `app.js` 的 localStorage 区域。接入真实后端时，替换登录、课程查询/创建、加入课程和发布动作即可，页面验收路径不需要改变。

## 创建链路

```text
浏览器 → Spring Boot API → 需求 Harness（澄清/确认）
                       └→ 规划 Harness（只读 ConfirmedIntent，生成树）
                                      ↓
                           Java 校验、布局、MySQL 事务落库
```

规划结果必须是至少 3 层、逐层扩展的有根树，每个节点包含 3–7 个具体任务。坐标、数据库 ID 和持久化均由 Java 后端控制；浏览器不会直连 MCP。

## MySQL 数据库

`database/schema.sql` 提供面向服务器部署的数据模型，数据库名称固定为 `BM-sql`。模型区分实验定义、学生实验实例和服务器代码工作区；源码正文保存在服务器持久化卷或对象存储中，数据库只保存文件索引、哈希和快照位置。

运行环境必须是 MySQL 8.0.16 或更高版本，因为更早的 MySQL 8.0 版本不会强制执行 `CHECK` 约束。请使用有权创建数据库和表的账号；命令中的 `--password` 会让 MySQL 客户端安全地交互式询问密码，不要把密码写进命令、脚本或仓库文件。

在项目根目录下使用 PowerShell 执行：

```powershell
mysql --user=root --password --execute="SOURCE database/schema.sql"
```

新数据库脚本使用 `CREATE DATABASE IF NOT EXISTS` 和 `CREATE TABLE IF NOT EXISTS`，可以对兼容结构安全重跑，不会主动删除数据库、表或现有数据。如果数据库已有旧结构，先执行增量迁移：

```powershell
mysql --user=root --password --execute="SOURCE database/migrations/20260911_add_experiment_creation.sql"
```

创建完成后执行只读验证：

```powershell
mysql --user=root --password --execute="SOURCE database/verify.sql"
```

验证结果中，数据库字符集、认证与创建流程所需表、关键字段和外键应显示 `OK`；`non_utf8mb4_table` 与 `unexpected_source_content_column` 两个结果集应为空。

如果 MySQL 运行在服务器 Docker 容器中，应把脚本通过标准输入传给容器内客户端，并从容器现有的安全配置中取得管理员凭据。不要为了运行脚本把管理员密码写进命令行，也不要重复安装一个会与现有 `3306` 端口冲突的 MySQL。部署完成后应重新执行验证，并用新的、不含密码的结果替换旧版部署记录。

节点依赖是否形成多节点环、用户是否有权启动某个实验，以及实验和运行状态能否跳转，必须由后端服务在事务中验证；仅靠静态外键和 `CHECK` 约束无法完整表达这些规则。

## 本地运行 Harness 与后端

要求 Java 17、Maven 3.9+ 和 Python 3.11+。首次安装 Python 依赖：

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r harnesses\requirements.txt
```

在三个终端分别启动需求 Harness、规划 Harness 和 Java API：

```powershell
.\.venv\Scripts\python.exe -m harnesses.requirements.service
.\.venv\Scripts\python.exe -m harnesses.planner.service
$env:DB_USER = 'bm_app'
$env:DB_PASSWORD = Read-Host 'MySQL password'
mvn -f backend/pom.xml spring-boot:run
```

随后启动前端并访问 `http://127.0.0.1:8000/`：

```powershell
python -m http.server 8000 --bind 127.0.0.1
```

默认 `HARNESS_PROVIDER=deterministic`，无需模型密钥，适合联调。接入 OpenAI-compatible 服务时，使用 `.env.example` 配置 `AI_BASE_URL`、`AI_API_KEY` 与两个模型名，且不要提交 `.env`。

Compose 只把 Java API 绑定到 `127.0.0.1`，两个 Harness 只在内部网络暴露。MySQL 使用既有实例；服务器反向代理、TLS 和正式鉴权属于下一阶段。

## API 与验证

创建流程依次调用 `/api/experiment-creation` 下的 `POST /sessions`、方向消息、方向确认、配置、计划生成和最终物化接口。最终物化请求必须提供 `Idempotency-Key`。这些业务接口要求先调用 `/api/auth/login` 建立 HttpOnly `BM_SESSION` Cookie；服务端从会话解析用户身份，不再接受 `X-User-Id` 或固定演示用户。退出调用 `/api/auth/logout`，当前用户可通过 `/api/auth/me` 查询。

```powershell
mvn -f backend/pom.xml test
.\.venv\Scripts\python.exe -m pytest harnesses
.\.venv\Scripts\python.exe -m harnesses.mcp_smoke
node --check app.js
docker compose config --quiet
```

## 前端课程发布演示

当前原生前端包含一套不依赖后端的登录、课程和实验发布演示流程。启动静态服务器后访问 `http://127.0.0.1:8000/`：

- 学生：`student@demo.com`，密码任意非空值；使用教师展示的 `SPRING01` 或新课程邀请码加入课程。
- 教师：`teacher@demo.com`，密码任意非空值；可创建课程、查看邀请码，并把实验库中的草稿发布到课程。
- 退出会清除当前登录会话；课程成员关系和发布记录仍保存在浏览器 localStorage 中，便于切换学生/教师身份验证同步结果。

教师发布后退出，使用学生账号加入相同课程，即可在学生工作台看到“已发布”实验并打开原有画布与编辑器。前端目前只把 `currentUser`、`courses`、`publishedExperiments` 和实验草稿存入 localStorage；后续接入后端时，替换 `app.js` 中的本地状态适配函数与登录/课程/发布动作即可，不改变页面验收路径或现有实验库、画布、编辑器交互。
