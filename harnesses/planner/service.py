from __future__ import annotations

import logging
import os
from typing import Any
from pydantic import ValidationError

from mcp.server import MCPServer
from starlette.requests import Request
from starlette.responses import JSONResponse

from harnesses.common.contracts import ConfirmedIntent, ExperimentTree, PlanNode, PlanTask
from harnesses.common.provider import ProviderError, generate_json


SYSTEM_PROMPT = """
你是实验流程规划 Harness。你只能把已经确认的 ConfirmedIntent 转换成实验流程节点，不能与学生对话、追问、修改需求或访问原始消息。
节点数量必须依据实验目标、工作量和技术依赖决定，严禁为了凑数量拆分节点或制造分支；自然的线性流程是合法的，只有真实并行工作才分支。
每个节点代表一个有明确产出的完整小阶段，而不是一个任务；每个节点包含 3–7 个按顺序执行、可以操作和验收的任务。
每个非根节点只有一个 parent_key，并且 prerequisite_keys 必须只包含该 parent_key；父节点完成全部任务后才解锁子节点。
unlock_rule 必须为 all_prerequisites_completed，completion_rule 必须为 all_tasks_completed；所有 key 稳定且唯一；父节点必须真实存在；不得成环。
任务必须使用“创建、实现、连接、配置、编写、验证、部署”等具体动作，不能只写“学习、了解、熟悉”。
节点预计分钟总和应落在给定 duration_minutes 内；difficulty 决定提示与独立完成程度。
不要输出数据库 ID、SQL、画布坐标、聊天内容或额外文字。严格返回 ExperimentTree JSON。
""".strip()

mcp = MCPServer("planner-harness")
logger = logging.getLogger(__name__)


def task(title: str, description: str, validation_type: str = "manual") -> PlanTask:
    return PlanTask(
        title=title,
        description=description,
        validation_type=validation_type,
        validation_config={},
    )


def deterministic_tree(intent: ConfirmedIntent, duration_minutes: int, difficulty: str) -> ExperimentTree:
    if duration_minutes <= 0:
        raise ValueError("duration_minutes must be positive")
    goal = intent.intent.observable_outcome.strip()
    title = (goal[:52] + "实验") if len(goal) <= 52 else (goal[:52] + "…")
    definitions = [
        ("goal", "目标与验收", "建立实验目标", "明确最终产物、边界和可验收结果", [
            task("整理实验目标", "把确认后的需求写成可观察的交付结果"),
            task("创建项目目录", "建立能够启动并持续扩展的项目结构"),
            task("编写验收清单", "把最终结果转换成逐项可检查的条件"),
        ]),
        ("foundation", "运行基础", "搭建可运行骨架", "准备前端、后端和数据库的最小运行链路", [
            task("配置运行环境", "安装依赖并确认开发命令可以执行", "command"),
            task("创建应用入口", "编写能够启动服务并返回基础页面的入口"),
            task("验证骨架启动", "启动项目并记录可重复的运行方式", "test"),
        ]),
        ("canvas", "界面阶段", "实现核心画布", "先完成用户可操作的画布和节点交互产物", [
            task("创建画布区域", "在页面中建立可承载实验节点的画布"),
            task("创建可拖动气泡", "实现节点拖动并更新画布中的位置"),
            task("显示节点详情卡片", "点击气泡后在右侧展示对应的详细介绍"),
        ]),
        ("navigation", "界面阶段", "完善详情呈现", "在已有详情卡片上建立可扩展的信息分区", [
            task("创建顶部分区导航", "在详情区域中建立介绍、任务和验收分区"),
            task("分块展示卡片内容", "按导航项将节点信息拆成可读的内容块"),
            task("绑定节点数据", "让导航和卡片随当前选中节点切换"),
        ]),
        ("api", "服务阶段", "建立后端接口", "把页面需要的节点和任务数据变成稳定接口", [
            task("创建节点查询接口", "返回节点、父子关系、任务和完成规则"),
            task("定义接口响应合同", "固定字段、状态码和错误响应格式"),
            task("验证接口数据", "请求接口并确认页面所需字段完整", "test"),
        ]),
        ("database", "数据阶段", "连接实验数据库", "让节点信息从 MySQL 真实读取并可持久化", [
            task("设计节点查询语句", "关联实验、节点、依赖和任务数据"),
            task("配置 MySQL 连接", "通过环境变量连接目标数据库"),
            task("验证数据持久化", "写入并重新读取数据确认链路有效", "test"),
        ]),
        ("acceptance", "验收阶段", "完成流程联调", "验证从节点完成到后续节点解锁的完整路径", [
            task("实现节点完成记录", "记录当前节点的任务完成状态"),
            task("实现前置解锁判断", "仅当前置节点全部完成时开放下一节点"),
            task("执行端到端验收", "按清单完成页面、接口和数据库联调", "test"),
        ]),
    ]
    context = " ".join([
        intent.intent.raw_request,
        intent.intent.observable_outcome,
        intent.intent.target_artifact,
        *intent.intent.constraints.required_stack,
    ]).lower()
    needs_ui = any(word in context for word in ("html", "页面", "画布", "节点", "web", "前端"))
    needs_detail = any(word in context for word in ("详情", "卡片", "导航", "拖动", "节点"))
    needs_service = any(word in context for word in ("后端", "接口", "python", "spring", "服务"))
    needs_data = any(word in context for word in ("mysql", "数据库", "数据", "sql"))
    success_criteria = intent.intent.success_criteria
    workload_signals = len(success_criteria) + len(intent.intent.constraints.required_stack)
    selected_keys = {"goal"}
    if workload_signals >= 2 or intent.intent.target_artifact.strip():
        selected_keys.add("foundation")
    if needs_ui:
        selected_keys.add("canvas")
    if needs_detail:
        selected_keys.add("navigation")
    if needs_service:
        selected_keys.add("api")
    if needs_data:
        selected_keys.add("database")
    if workload_signals >= 3 or any("验收" in item or "验证" in item for item in success_criteria):
        selected_keys.add("acceptance")
    node_specs = []
    parent = None
    for key, stage, node_title, description, tasks in definitions:
        if key not in selected_keys:
            continue
        depth = 0 if parent is None else node_specs[-1][2] + 1
        node_specs.append((key, parent, depth, stage, node_title, description, tasks))
        parent = key
    guidance = {
        "beginner": "提供完整骨架、操作提示和示例",
        "easy": "提供关键骨架与少量提示",
        "normal": "提供接口约定，关键实现由学生完成",
        "hard": "只提供必要上下文和验收标准",
        "challenge": "只提供目标、约束和自动验收入口",
    }[difficulty]
    per_node = min(1440, max(1, duration_minutes // len(node_specs)))
    nodes = [PlanNode(
        key=key,
        parent_key=parent,
        prerequisite_keys=[] if parent is None else [parent],
        unlock_rule="all_prerequisites_completed",
        completion_rule="all_tasks_completed",
        depth=depth,
        stage=stage,
        title=node_title,
        description=f"{description}；{guidance}",
        estimated_minutes=per_node,
        tasks=tasks,
    ) for key, parent, depth, stage, node_title, description, tasks in node_specs]
    return ExperimentTree(title=title, purpose=[goal, "按真实依赖逐阶段完成并解锁后续节点"], nodes=nodes)


def _validate_model_tree(result: Any, duration_minutes: int) -> ExperimentTree:
    tree = ExperimentTree.model_validate(result)
    if sum(node.estimated_minutes for node in tree.nodes) > max(duration_minutes + 60, duration_minutes * 1.2):
        raise ValueError("generated plan exceeds duration budget")
    return tree


@mcp.tool()
async def generate_experiment_tree(
    confirmed_intent: dict[str, Any],
    duration_minutes: int,
    difficulty: str,
) -> ExperimentTree:
    """Generate a dependency-aware experiment flow from a confirmed intent only."""
    intent = ConfirmedIntent.model_validate(confirmed_intent)
    if duration_minutes <= 0:
        raise ValueError("duration_minutes must be positive")
    if difficulty not in {"beginner", "easy", "normal", "hard", "challenge"}:
        raise ValueError("unsupported difficulty")
    if os.getenv("HARNESS_PROVIDER", "deterministic") == "deterministic":
        return deterministic_tree(intent, duration_minutes, difficulty)
    request_payload = {
        "confirmed_intent": intent.model_dump(mode="json"),
        "duration_minutes": duration_minutes,
        "difficulty": difficulty,
    }
    schema = ExperimentTree.model_json_schema()
    try:
        result = await generate_json(SYSTEM_PROMPT, request_payload, "PLANNER_MODEL", schema)
    except ProviderError as error:
        if str(error) != "model returned invalid JSON":
            raise
        result = {"_model_output_invalid": True}
    try:
        tree = _validate_model_tree(result, duration_minutes)
    except (ValidationError, ValueError) as error:
        validation_errors = (
            error.errors(include_url=False)
            if isinstance(error, ValidationError)
            else [{"type": "business_validation", "msg": str(error)}]
        )
        repair_payload = {
            "request": request_payload,
            "invalid_output": result,
            "validation_errors": validation_errors,
            "instruction": "只修复合同、树结构和任务格式，不改变 ConfirmedIntent。",
        }
        try:
            repaired = await generate_json(
                SYSTEM_PROMPT + "\n当前请求是格式修复，必须返回完整合法实验树。",
                repair_payload,
                "PLANNER_MODEL",
                schema,
            )
        except ProviderError as second_error:
            logger.warning(
                "planner model repair failed; using safe fallback (failure=%s)",
                type(second_error).__name__,
            )
            return deterministic_tree(intent, duration_minutes, difficulty)
        try:
            tree = _validate_model_tree(repaired, duration_minutes)
        except (ValidationError, ValueError) as second_error:
            logger.warning(
                "planner model output failed contract or budget validation twice; using safe fallback "
                "(failure=%s)",
                type(second_error).__name__,
            )
            tree = deterministic_tree(intent, duration_minutes, difficulty)
    return tree


@mcp.custom_route("/health", methods=["GET"])
async def health(_: Request) -> JSONResponse:
    if os.getenv("HARNESS_PROVIDER", "deterministic") == "openai-compatible" and (
        not os.getenv("AI_API_KEY") or not os.getenv("PLANNER_MODEL")
    ):
        return JSONResponse({"status": "not_ready", "harness": "planner"}, status_code=503)
    return JSONResponse({"status": "ok", "harness": "planner"})


if __name__ == "__main__":
    mcp.run(
        transport="streamable-http",
        host=os.getenv("HARNESS_HOST", "127.0.0.1"),
        port=int(os.getenv("PORT", "8102")),
        stateless_http=True,
        json_response=True,
    )
