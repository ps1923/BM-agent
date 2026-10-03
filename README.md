# 学习画布

这是一个面向毕业设计的智能编程实验教学系统：教师或学生通过三段式向导创建树状学习实验，学生在独立工作区编写和运行代码，教师通过课程管理查看学习过程。创建前先讨论实验方向，再填写周期与难度，最后预览并确认实验树；确认前不会写入正式实验，确认后由后端一次性创建阶段、节点、任务与连线。

- 点阵画布与可拖动节点
- 节点显示序号、绿色状态点和学习介绍
- 点击节点后从右侧打开用时、完成度、文件位置和名称
- 底部保留「开始攻克节点」主按钮样式
- 点击「开始攻克节点」进入黑底编辑器布局：任务、代码、智能体、终端与顶部工作区栏
- 首屏按当前登录用户从服务端加载个人实验；点击实验进入对应画布，画布可返回实验库
- 最近打开区域只记录本次页面访问中的个人实验，不伪称跨登录持久化
- 文件库右侧「＋」打开三段式 Agent 创建向导：方向对话、整数周期、五档节点难度
- 只有第一段围绕学习方向进行对话，后两段只提供周期输入和难度选择
- 需求 Harness 只读取本会话的方向对话，只做事实提取、追问和需求确认
- 规划 Harness 只接收带版本和哈希的 `ConfirmedIntent`、周期及难度，只生成实验树
- 两个 Harness 是独立 Python MCP Streamable HTTP 服务，使用不同进程、端口、提示词和唯一工具
- 确认后由 Spring Boot 校验并布局实验树，再通过 MySQL 事务创建阶段、节点、任务与连线
- 个人实验列表提供真实加载、空状态、错误重试；未实现持久化的分组与删除入口不展示
- 编辑器终端已经通过后端受限命令接口连接独立工作区；普通代码指导、Bug 案例审核、补丁确认、异步深度诊断和教师过程管理均已接入真实后端接口，不得用前端假数据代替

## 前端角色与课程流程

当前浏览器页面已包含真实认证、课程和实验发布入口：

- 学生和教师账号由服务端管理员创建，并通过 BCrypt 密码哈希认证
- 服务端通过 HttpOnly 会话 Cookie 识别当前用户，前端不保存密码、Token 或角色选择
- 教师可创建课程并获得邀请码；学生可使用邀请码加入课程
- 教师选择自己的实验草稿发布到课程后，课程成员才能访问该实验
- 学生创建的个人实验不自动进入教师课程
- `GET /api/experiments/mine` 只返回当前登录用户创建且未归档的实验；学生可运行自己的私有草稿，教师在课程管理中发布自己的实验
- 当前课程、成员和发布数据来自 MySQL；后端不可用时页面必须显示明确错误，不回退到假数据

页面验收路径包括“登录 → 个人实验库 → 创建/刷新恢复 → 学生运行个人草稿”，以及“教师创建课程并发布 → 学生加入课程并运行课程实验”；每一步都必须经过后端权限校验。

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

当前版本在已有创建流程结构上还需要按顺序执行以下增量迁移；每条命令都应在目标数据库管理员确认后执行，不能把密码写入命令行：

```text
database/migrations/20260918_add_auth.sql
database/migrations/20260918_add_courses.sql
database/migrations/20260918_add_bug_cases.sql
database/migrations/20260918_add_code_patches.sql
database/migrations/20260918_add_teacher_reviews.sql
database/migrations/20260918_add_diagnosis_tasks.sql
database/migrations/20261001_add_node_linked_workspace_snapshots.sql
database/migrations/20261001_backfill_task_progress.sql
database/migrations/20261001_fix_rejected_patch_timestamp_constraint.sql
database/migrations/20261001_add_workspace_starting_state.sql
```

迁移完成后先执行 `database/verify.sql`，确认认证、课程、Bug、补丁、诊断任务、教师评价和阶段快照结构均为 `OK`，并确认 `task_progress_status` 为 `OK`、`inconsistent_task_progress_rows` 为 `0`，课程关键 CHECK 定义和 `ENFORCED` 状态匹配，再启动后端和前端。验证脚本会显式限定 `BM-sql`，缺少必要进度表时报告 `SKIPPED_MISSING_SCHEMA_OR_TABLE`；没有完成迁移时，不得进行正向页面验收。

创建完成后执行只读验证：

```powershell
mysql --user=root --password --execute="SOURCE database/verify.sql"
```

验证结果中，数据库字符集、认证与创建流程所需表、关键字段和外键应显示 `OK`；`non_utf8mb4_table` 与 `unexpected_source_content_column` 两个结果集应为空。

如果 MySQL 运行在服务器 Docker 容器中，应把脚本通过标准输入传给容器内客户端，并从容器现有的安全配置中取得管理员凭据。不要为了运行脚本把管理员密码写进命令行，也不要重复安装一个会与现有 `3306` 端口冲突的 MySQL。部署完成后应重新执行验证，并用新的、不含密码的结果替换旧版部署记录。

节点依赖是否形成多节点环、用户是否有权启动某个实验，以及实验和运行状态能否跳转，必须由后端服务在事务中验证；仅靠静态外键和 `CHECK` 约束无法完整表达这些规则。

## 本地运行 Harness、后端与工作区

要求 Java 17、Maven 3.9+ 和 Python 3.11+。首次安装 Python 依赖：

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r harnesses\requirements.txt
```

在三个终端分别启动需求 Harness、规划 Harness 和 Java API；如果要验收学生工作区，还必须准备可用的 Docker Engine 和工作区运行镜像：

```powershell
.\.venv\Scripts\python.exe -m harnesses.requirements.service
.\.venv\Scripts\python.exe -m harnesses.planner.service
$env:DB_USER = 'bm_app'
$env:DB_PASSWORD = Read-Host 'MySQL password'
mvn -f backend/pom.xml spring-boot:run
```

后端启动前必须通过环境变量提供数据库密码，不能把密码写入命令、日志或仓库。工作区运行时默认使用 `WORKSPACE_ROOT`、`WORKSPACE_RUNTIME_IMAGE`、`WORKSPACE_CPU_LIMIT`、`WORKSPACE_MEMORY_LIMIT_MB` 等环境变量配置资源限制。终端输出由 `WORKSPACE_MAX_OUTPUT_BYTES` 限制（默认 65536 字节，最大 1048576 字节），超出部分会被丢弃并标记为已截断。

随后启动前端并访问 `http://127.0.0.1:8000/`：

```powershell
python -m http.server 8000 --bind 127.0.0.1
```

默认 `HARNESS_PROVIDER=deterministic`，无需模型密钥，适合联调。Harness 使用 `AI_BASE_URL`、`AI_API_KEY` 和两个 Harness 模型名；普通代码指导使用 `DEEPSEEK_BASE_URL`、`DEEPSEEK_API_KEY`、`DEEPSEEK_MODEL`。这些变量只通过环境变量提供，且不要提交 `.env`。

Compose 只把 Java API 绑定到 `127.0.0.1`，两个 Harness 只在内部网络暴露。MySQL 使用既有实例。前端在本地 `127.0.0.1:8000` 或 `localhost:8000` 联调时连接本机 `:8080/api`；其他来源默认访问同源 `/api`。可通过页面加载 `app.js` 之前设置 `window.BM_API_BASE_URL` 覆盖 API 基址，HTTPS 页面不允许覆盖到明文 HTTP 地址。

远程浏览器不能访问服务器的 `127.0.0.1:8080`。生产页面使用同源 `/api`，因此必须在**现有 HTTPS Nginx 虚拟主机中增量加入**代理规则；不要用下例替换当前站点配置，也不要将 8080 暴露到公网：

```nginx
location ^~ /api/ {
    proxy_pass http://127.0.0.1:8080;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_read_timeout 150s;
}
```

`proxy_pass` 不带 URI 后缀，确保 `/api/...` 路径原样送到 Spring API。使用 HTTPS 时，后端环境需设置 `AUTH_SECURE_COOKIE=true`、`AUTH_COOKIE_SAME_SITE=Lax`，并将 `ALLOWED_ORIGIN` 配置为精确的站点 origin（例如 `https://教学系统域名`）。跨源 API 覆盖还要求后端允许精确前端 origin；`SameSite=Lax` Cookie 不应被设计为跨站认证方案。Nginx 规则、TLS 和生产环境变量都必须先检查现有服务器配置、取得维护授权后再应用。

本次代码变更只提供配置逻辑和部署示例，未修改云服务器、Nginx、TLS 或生产环境变量；公网部署状态仍待真实环境验收。生产验收需通过真实 HTTPS 页面登录，确认登录响应的会话 Cookie 同时具有 `Secure`、`HttpOnly`、`SameSite=Lax`，随后 `/api/me` 成功恢复会话；浏览器网络请求目标应为当前站点的 `/api/...`，不得请求访问者设备的 loopback 地址。项目中实现了 Cookie 会话认证，但若尚未配置并验收 TLS 与反向代理，不应宣称公网部署已完成。

## API 与验证

创建流程依次调用 `/api/experiment-creation` 下的 `POST /sessions`、方向消息、方向确认、配置、计划生成和最终物化接口。最终物化请求必须提供 `Idempotency-Key`。这些业务接口要求先调用 `/api/auth/login` 建立 HttpOnly `BM_SESSION` Cookie；服务端从会话解析用户身份，不再接受 `X-User-Id` 或固定演示用户。退出调用 `/api/auth/logout`，当前用户可通过 `/api/me` 查询；`/api/auth/me` 保留为兼容接口。

个人实验库通过 `GET /api/experiments/mine` 按 `creator_id` 返回当前用户未归档实验，按更新时间倒序；它不把仅通过课程发布获得访问权的实验复制进个人列表。工作区启动时，学生可运行本人创建的 `draft` 实验，其他学生仍需满足已发布课程及有效成员权限。个人实验分组、归档和删除暂未提供服务端持久化能力，因此界面不显示会造成已保存错觉的管理入口。

学生工作区就绪后可调用 `POST /api/runs/{runId}/guidance` 请求普通代码指导。后端按运行实例权限读取实验、当前节点、索引文件和最近对话，再调用 DeepSeek OpenAI-compatible `/chat/completions`；上下文采用结构化 JSON，数据内容按不可信输入处理，并受 `GUIDANCE_MAX_CONTEXT_CHARS` 总字符预算限制（不等同于模型 Token 数），超限时优先保留学生问题并缩减代码/历史/RAG 内容。未配置模型时返回明确错误，不生成演示答案。RAG 是否可用由 Embedding 和 Chroma 的运行状态决定，未配置时返回 `ragAvailable=false`。

Bug 案例使用 `POST /api/runs/{runId}/bugs` 保存，教师使用 `GET /api/bugs?courseId={id}` 和 `POST /api/bugs/{id}/approve` 审核。审核通过后，后端使用本地 `BAAI/bge-small-zh-v1.5` 的 OpenAI-compatible Embedding 服务写入 Chroma；Bug 案例保存可选的标准化技术栈，指导和深度诊断按课程、实验、当前节点、技术栈和审核状态检索，再通过 MySQL 权限与节点/技术栈兼容性进行二次校验；通用案例可被同技术栈复用。Embedding 或 Chroma 不可用时，指导会降级为无 RAG，并返回 `ragAvailable=false`。

学生先通过 `GET /api/runs/{runId}/patches/{patchId}` 读取自己运行实例中的补丁详情，前端同时读取当前文件并展示“当前内容/建议内容”差异及哈希状态；只有点击确认后才调用 `POST /api/runs/{runId}/patches/{patchId}/apply`。服务端再次校验学生身份、补丁状态、文件路径和每个文件的当前哈希，再创建应用前快照；哈希不一致时拒绝应用，学生不能通过前端绕过确认直接修改补丁。

深度诊断使用 `POST /api/runs/{runId}/deep-diagnosis` 创建异步任务，使用 `GET /api/diagnoses/{diagnosisId}` 轮询 `queued/running/completed/failed/cancelled` 状态。任务只扫描受支持的源码、配置和测试文件，排除常见敏感文件名并对常见凭据格式脱敏；上下文采用 JSON 数据边界，限制总字符数、单文件大小、文件数、并发线程和排队数量。被截断或脱敏的文件可用于问题定位，但不能作为补丁目标。模型必须返回结构化 JSON，服务端校验引用路径、完整文件资格、当前 SHA-256 和补丁大小后才保存补丁；前端只展示补丁差异入口，学生确认后才调用补丁应用接口。通用凭据模式扫描不保证识别自定义密钥，学生仍应避免把真实凭据放入工作区。

教师可以通过 `/api/teacher/courses/{id}/dashboard` 查看课程成员和实验运行进度，通过 `/api/teacher/runs/{id}/timeline`、`/summary` 查看过程记录和统计，并通过 `/reviews` 保存最终评价。统计摘要只返回真实数据库聚合结果，AI 摘要未生成时标记 `aiSummaryAvailable=false`，不伪造结论。

教师工作台已经接入上述看板接口：课程选择会恢复最近查看的课程，学生表支持空数据和未开始状态，已开始的运行实例可展开时间线、节点/命令/对话/Bug 统计，并提交 0–100 分及文字评价。教师查看运行摘要时，后端仅将统计和事件元数据交给 DeepSeek 生成辅助摘要；模型不可用时返回 `aiSummaryAvailable=false`，页面明确显示真实统计，不把 AI 摘要当作教师评价。

教师工作台的“Bug 案例审核”区域按当前课程加载学生提交的案例；教师可以填写审核意见并选择“通过并入库”或“拒绝”。通过后由后端建立 Chroma 向量索引，页面展示审核状态和 RAG 索引状态。课程切换时会阻止旧请求覆盖当前课程，后端仍负责最终的课程权限校验。

学生端会在当前浏览会话保存最近打开的实验编号；刷新后先通过 `/api/me` 恢复身份，再从后端重新读取该实验，避免恢复到过期的静态演示数据。退出登录会清除该会话状态。

```powershell
mvn -f backend/pom.xml test
.\.venv\Scripts\python.exe -m pytest
.\.venv\Scripts\python.exe -m harnesses.mcp_smoke
node --test tests/api-config.test.js
node --check app.js
docker compose config --quiet
```

## 前端课程发布验收

启动静态服务器和后端后访问 `http://127.0.0.1:8000/`。使用管理员预置的教师账号创建课程并发布实验，再使用学生账号通过邀请码加入课程。刷新页面后，课程、成员和发布结果应仍来自服务端；未登录用户、其他课程教师和非成员学生不得访问受保护数据。
