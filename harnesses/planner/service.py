from __future__ import annotations

import logging
import os
from pathlib import Path
from typing import Any
from pydantic import ValidationError

from mcp.server import MCPServer
from starlette.requests import Request
from starlette.responses import JSONResponse

from harnesses.common.contracts import ConfirmedIntent, ExperimentTree, PlanNode, PlanTask
from harnesses.common.provider import ProviderError, generate_json


PLANNER_SKILL_PATH = Path(__file__).with_name("skills") / "node-planning.md"
MIN_USEFUL_STAGE_MINUTES = 15
MAX_FALLBACK_NODE_COUNT = 80
MAX_NODE_MINUTES = 1440


def load_node_planning_skill() -> str:
    """Load the planner's project-owned node design rules explicitly at runtime."""
    try:
        return PLANNER_SKILL_PATH.read_text(encoding="utf-8").strip()
    except OSError as error:
        raise RuntimeError("planner node-planning skill is missing or unreadable") from error


SYSTEM_PROMPT = """
你是实验流程规划 Harness。你只能把已经确认的 ConfirmedIntent 转换成实验流程节点，不能与学生对话、追问、修改需求或访问原始消息。
节点数量、递归细分、学生视角的粒度、分支和任务设计必须遵循随后附带的 Planner Skill；该 Skill 是实验创建时的一次性规划规范，不是让你再次向学生追问。
每个非根节点只有一个 parent_key，并且 prerequisite_keys 必须只包含该 parent_key；父节点完成全部任务后才解锁子节点。
unlock_rule 必须为 all_prerequisites_completed，completion_rule 必须为 all_tasks_completed；所有 key 稳定且唯一；父节点必须真实存在；不得成环。
节点预计分钟总和应落在给定 duration_minutes 内；difficulty 决定提示与独立完成程度。
不要输出数据库 ID、SQL、画布坐标、聊天内容或额外文字。严格返回 ExperimentTree JSON。
""".strip()


def planner_system_prompt() -> str:
    return f"{SYSTEM_PROMPT}\n\n<planner_skill>\n{load_node_planning_skill()}\n</planner_skill>"


mcp = MCPServer("planner-harness")
logger = logging.getLogger(__name__)


def task(title: str, description: str, validation_type: str = "manual") -> PlanTask:
    return PlanTask(
        title=title,
        description=description,
        validation_type=validation_type,
        validation_config={},
    )


def _allocate_node_minutes(duration_minutes: int, node_count: int) -> list[int]:
    """Allocate a positive integer budget deterministically without exceeding node caps."""
    if duration_minutes <= 0 or node_count <= 0:
        raise ValueError("duration and node count must be positive")
    count = min(node_count, duration_minutes)
    base = min(MAX_NODE_MINUTES, duration_minutes // count)
    remainder = min(duration_minutes - base * count, (MAX_NODE_MINUTES - base) * count)
    extra_per_node, extra_remainder = divmod(remainder, count)
    return [
        base + extra_per_node + (1 if index < extra_remainder else 0)
        for index in range(count)
    ]


def _criteria_have_explicit_order(criteria: list[str]) -> bool:
    ordering_markers = ("完成后", "之后", "前置", "依赖于", "依赖", "才能", "先完成", "after", "before", "depends on", "requires", "then")
    return any(
        marker in criterion.casefold()
        for criterion in criteria
        for marker in ordering_markers
    )


def deterministic_tree(intent: ConfirmedIntent, duration_minutes: int, difficulty: str) -> ExperimentTree:
    if duration_minutes <= 0:
        raise ValueError("duration_minutes must be positive")
    if duration_minutes < MIN_USEFUL_STAGE_MINUTES:
        raise ValueError("duration_minutes must be at least 15 to retain meaningful practice and verification")
    goal = intent.intent.observable_outcome.strip() or intent.intent.target_artifact.strip()
    artifact = intent.intent.target_artifact.strip() or goal
    criteria = list(dict.fromkeys(
        item.strip() for item in intent.intent.success_criteria if item.strip()
    ))
    if not artifact or not criteria:
        raise ValueError("confirmed intent must include an observable outcome and non-empty success criteria")
    constraints = intent.intent.constraints
    current_state = intent.intent.current_state
    known_skills = "、".join(item.strip() for item in current_state.known_skills if item.strip()) or "未记录"
    starting_assets = "、".join(item.strip() for item in current_state.starting_assets if item.strip()) or "无已确认的现成素材"
    stack_items = [item.strip() for item in constraints.required_stack if item.strip()]
    required_stack = "、".join(stack_items) or "未指定；沿用现有可用技术，不新增技术栈"
    has_required_stack = bool(stack_items)
    excluded_scope = "、".join(item.strip() for item in constraints.excluded_scope if item.strip()) or "无额外排除项"
    criteria_text = "；".join(criteria)
    experience_text = current_state.relevant_experience.strip() or "未记录"
    environment_text = "、".join(item.strip() for item in constraints.environment if item.strip()) or "沿用现有学习环境"
    title = (goal[:52] + "实验") if len(goal) <= 52 else (goal[:52] + "…")

    guidance = {
        "beginner": "提供完整骨架、操作提示和示例",
        "easy": "提供关键骨架与少量提示",
        "normal": "提供接口约定，关键实现由学生完成",
        "hard": "只提供必要上下文和验收标准",
        "challenge": "只提供目标、约束和自动验收入口",
    }[difficulty]
    personalized_guidance = (
        f"{guidance}；复用学生已具备能力：{known_skills}（经验：{experience_text}）；"
        f"先检查并复用现有素材：{starting_assets}。"
    )

    root_tasks = [
        task("明确本次最小成果", f"用自己的话写明本次要交付的成果：{goal}；交付物为：{artifact}，并记录对应说明作为验收依据"),
        task("整理可检查标准", f"按确认顺序列出独立检查项：{criteria_text}；用清单保存，供后续成果节点逐项对照"),
        task("记录约束与已有基础", f"注明已确认技术栈：{required_stack}；环境：{environment_text}；排除范围：{excluded_scope}；能力：{known_skills}；经验：{experience_text}；素材：{starting_assets}"),
    ]
    if duration_minutes < 30:
        root_title = "端到端最小成果"
        root_description = f"周期不足 30 分钟，本兜底计划只覆盖第一项已确认验收标准“{criteria[0]}”，其余标准不得标记为已完成；遵守技术栈“{required_stack}”和排除范围“{excluded_scope}”。"
        root_tasks = [
            task("选择本轮可验证标准", f"仅将第一项确认标准“{criteria[0]}”作为本轮范围；其余 {len(criteria) - 1} 项留待扩展周期", "manual"),
            task("实现最小可检查结果", f"围绕该标准产出“{artifact}”的一项可观察结果：{criteria[0]}；不得添加未确认技术或功能"),
            task("验证并保存证据", f"通过测试、运行输出或人工观察记录该标准的结果；未覆盖标准仍保持未完成：{criteria_text}", "manual"),
        ]
    else:
        root_title = "目标与验收"
        root_description = f"先确定交付物“{artifact}”及验收清单；后续每条验收标准形成单独成果节点。"

    stage_capacity = min(MAX_FALLBACK_NODE_COUNT, duration_minutes // MIN_USEFUL_STAGE_MINUTES)
    environment_items = [item.strip() for item in constraints.environment if item.strip()]
    environment_ready = has_required_stack and all(
        any(stack.casefold() in environment.casefold() for environment in environment_items)
        for stack in stack_items
    )
    setup_needed = (
        has_required_stack
        and not environment_ready
        and stage_capacity >= 3
    )
    node_specs = [("goal", None, 0, "目标与验收", root_title, root_description, root_tasks)]
    shared_parent = "goal"
    shared_depth = 1
    if setup_needed:
        setup_tasks = [
            task("检查项目起点", f"确认工作区内是否有可复用项目；已确认基础为：{known_skills}；现成素材记录为：{starting_assets}"),
            task("准备已确认运行条件", f"仅准备明确要求的技术栈：{required_stack}；不得引入未确认框架或服务", "command"),
            task("运行最小环境检查", f"执行对应的启动或测试命令，保存结果，确认环境能支持“{artifact}”", "test"),
        ]
        node_specs.append(("setup", "goal", 1, "准备工作环境", "准备并验证最小环境", f"复用已有项目与能力，仅补齐目标“{goal}”明确需要的运行条件。", setup_tasks))
        shared_parent = "setup"
        shared_depth = 2
    elif has_required_stack and not environment_ready:
        root_description += f" 技术栈准备时间有限，须在本阶段先确认并准备“{required_stack}”，再实施验收标准；排除范围：{excluded_scope}。"
        root_tasks.extend([
            task("检查并准备必要环境", f"确认现有环境能运行已确认技术栈：{required_stack}；缺少部分仅补齐必需项，不安装未确认组件", "command"),
            task("验证环境与成果", f"先完成最小运行环境检查，再验证交付物“{artifact}”是否满足当前验收标准"),
        ])

    ordered_criteria = _criteria_have_explicit_order(criteria)
    max_depth_criteria = 9 - shared_depth if ordered_criteria else len(criteria)
    criterion_capacity = min(
        len(criteria), stage_capacity - len(node_specs), MAX_FALLBACK_NODE_COUNT - len(node_specs),
        max_depth_criteria,
    )
    selected_criteria = criteria[:max(0, criterion_capacity)]
    if stage_capacity == 1:
        selected_criteria = []
    for index, criterion in enumerate(selected_criteria, start=1):
        criterion_title = criterion if len(criterion) <= 125 else criterion[:124] + "…"
        criterion_tasks = [
            task("明确验收证据", f"为标准“{criterion}”确定可观察的输入、输出、代码位置或人工检查证据"),
            task("实现该项最小行为", f"只实现直接满足标准“{criterion}”所需的行为；产出需属于“{artifact}”，并遵守排除范围：{excluded_scope}"),
            task("运行检查并记录结果", f"针对标准“{criterion}”运行测试、示例或人工检查；保存结果，失败时修正后重验", "test"),
        ]
        parent = node_specs[-1][0] if ordered_criteria and index > 1 else shared_parent
        depth = node_specs[-1][2] + 1 if ordered_criteria and index > 1 else shared_depth
        node_specs.append((
            f"criterion-{index}", parent, depth, "成果实现与验证",
            f"实现并验证：{criterion_title}", f"独立完成并自验一项已确认成功标准：“{criterion}”。",
            criterion_tasks,
        ))

    omitted_count = len(criteria) - (1 if stage_capacity == 1 else len(selected_criteria))
    purpose = [goal]
    if stage_capacity == 1 and omitted_count > 0:
        purpose.append(f"短周期兜底仅覆盖第一项验收标准；另有 {omitted_count} 项需增加周期后完成")
    elif stage_capacity == 1:
        purpose.append("短周期使用单一端到端节点覆盖唯一验收标准")
    elif omitted_count > 0:
        purpose.append(f"兜底计划按确认顺序覆盖部分验收标准；另有 {omitted_count} 项需增加周期后完成")
    else:
        purpose.append("每条确认的验收标准均有独立成果和验证节点")

    minute_allocations = _allocate_node_minutes(duration_minutes, len(node_specs))
    task_guidance = {
        "beginner": "先按任务顺序完成，每步对照清单并保存示例或运行证据。",
        "easy": "优先复用已有骨架；每个任务完成后进行一次对应自检。",
        "normal": "自行完成实现，并为当前验收标准保留可复核证据。",
        "hard": "自主选择实现方案，严格按对应验收标准验证。",
        "challenge": "独立选择并论证实现路径，以自动化结果或明确观察证据证明完成。",
    }[difficulty]
    nodes = []
    for index, (key, parent, depth, stage, node_title, description, tasks) in enumerate(node_specs):
        guided_tasks = [
            PlanTask(
                title=item.title,
                description=f"{item.description}；{task_guidance}",
                validation_type=item.validation_type,
                validation_config=item.validation_config,
            )
            for item in tasks
        ]
        nodes.append(PlanNode(
            key=key,
            parent_key=parent,
            prerequisite_keys=[] if parent is None else [parent],
            unlock_rule="all_prerequisites_completed",
            completion_rule="all_tasks_completed",
            depth=depth,
            stage=stage,
            title=node_title,
            description=f"{description}；{personalized_guidance}",
            estimated_minutes=minute_allocations[index],
            tasks=guided_tasks,
        ))
    return ExperimentTree(title=title, purpose=purpose, nodes=nodes)


def _validate_model_tree(result: Any, duration_minutes: int) -> ExperimentTree:
    tree = ExperimentTree.model_validate(result)
    if sum(node.estimated_minutes for node in tree.nodes) > duration_minutes:
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
    if duration_minutes < MIN_USEFUL_STAGE_MINUTES:
        raise ValueError("duration_minutes must be at least 15 to retain meaningful practice and verification")
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
        system_prompt = planner_system_prompt()
        result = await generate_json(system_prompt, request_payload, "PLANNER_MODEL", schema)
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
                system_prompt + "\n当前请求是格式修复，必须返回完整合法实验树。",
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
