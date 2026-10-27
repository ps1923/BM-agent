from __future__ import annotations

import json
import logging
import os
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import httpx
from pydantic import ValidationError

from mcp.server import MCPServer
from mcp.server.mcpserver.exceptions import ToolError
from starlette.requests import Request
from starlette.responses import JSONResponse

from harnesses.common.contracts import AcceptanceCriterion, ConfirmedIntent, ExperimentTree, PlanNode, PlanTask
from harnesses.common.provider import ProviderError, generate_json


PLANNER_SKILL_PATH = Path(__file__).with_name("skills") / "node-planning.md"
MIN_USEFUL_STAGE_MINUTES = 15
MAX_FALLBACK_NODE_COUNT = 80
MAX_NODE_MINUTES = 1440


def load_node_planning_skill() -> str:
    """Load the planner's project-owned node design rules explicitly at runtime."""
    try:
        content = PLANNER_SKILL_PATH.read_text(encoding="utf-8").strip()
    except (OSError, UnicodeError) as error:
        raise RuntimeError("planner node-planning skill is missing or unreadable") from error
    if not content:
        raise RuntimeError("planner node-planning skill is empty")
    return content


SYSTEM_PROMPT = """
你是实验流程规划 Harness。你只能把已经确认的 ConfirmedIntent 转换成实验流程节点，不能与学生对话、追问、修改需求或访问原始消息。
节点数量、递归细分、学生视角的粒度、分支、任务和节点验收准则必须遵循随后附带的 Planner Skill；该 Skill 是实验创建时的一次性规划规范，不是让你再次向学生追问。
每条已确认 success_criteria 必须在可执行成果节点中按原文可追溯；若本次预算或单父节点结构无法覆盖，必须在 purpose 中原文列出并明确标注“未覆盖”，不得静默漏项或将准备清单说成成果已完成。
包含多个可独立验收结果或明显跨能力的 success_criteria 必须有保留原文的父节点并继续细分；明确列出的子成果分别落入独立后代节点。预算不足时明确标注未覆盖，不得把宽泛复合结果留作叶节点。
每个非根节点只有一个 parent_key，并且 prerequisite_keys 必须只包含该 parent_key；父节点完成全部任务后才解锁子节点。
unlock_rule 必须为 all_prerequisites_completed，completion_rule 必须为 all_tasks_completed；所有 key 稳定且唯一；父节点必须真实存在；不得成环。
每节点预计时间是学习量级估算，duration_minutes 是范围与节奏参考；不得因整树估算用时超出该值而拒绝计划或删减已确认成果。单节点预计时间须为 1–1440 分钟。difficulty 决定提示与独立完成程度。
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


def _criteria_for_tasks(tasks: list[PlanTask]) -> list[AcceptanceCriterion]:
    methods = {
        "command": "test_artifact",
        "test": "test_artifact",
        "agent": "static_code",
        "manual": "manual_demo",
    }
    return [
        AcceptanceCriterion(
            criterion=item.title,
            verification_method=methods.get(item.validation_type, "manual_demo"),
            expected_evidence=item.description,
        )
        for item in tasks
    ]


def _ensure_node_acceptance_criteria(tree: ExperimentTree) -> ExperimentTree:
    """Backfill explicit criteria for older planner responses and deterministic fallback nodes."""
    nodes = [
        node if node.acceptance_criteria else node.model_copy(
            update={"acceptance_criteria": _criteria_for_tasks(node.tasks)}
        )
        for node in tree.nodes
    ]
    return tree.model_copy(update={"nodes": nodes})


def _allocate_node_minutes(duration_minutes: int, node_count: int) -> list[int]:
    """Allocate a positive integer budget deterministically without exceeding node caps."""
    if duration_minutes <= 0 or node_count <= 0:
        raise ValueError("duration and node count must be positive")
    base = max(1, min(MAX_NODE_MINUTES, duration_minutes // node_count))
    remainder = min(max(0, duration_minutes - base * node_count), (MAX_NODE_MINUTES - base) * node_count)
    extra_per_node, extra_remainder = divmod(remainder, node_count)
    return [
        base + extra_per_node + (1 if index < extra_remainder else 0)
        for index in range(node_count)
    ]


def _environment_confirms_stack(stack: str, environment_items: list[str]) -> bool:
    stack_pattern = rf"(?<![a-z0-9]){re.escape(stack.casefold())}(?![a-z0-9])"
    unavailable_markers = (
        "未安装", "没有安装", "尚未安装", "未配置", "没有配置", "尚未配置", "未就绪", "不可用", "不可执行", "缺少",
        "not installed", "isn't installed", "isn't currently installed", "not currently installed", "not yet installed",
        "not configured", "isn't configured", "not available", "isn't available",
        "not present", "isn't present", "not enabled", "not executable", "not ready", "isn't ready",
        "not usable", "not working", "unavailable", "missing",
    )
    unknown_markers = ("未知", "未确认", "unknown", "unconfirmed", "not sure")
    available_markers = (
        "已安装", "安装完成", "已配置", "配置完成", "已就绪", "已验证", "可用", "可执行", "运行正常", "可运行",
        "installed", "configured", "available", "ready", "present", "working", "enabled", "executable",
    )
    pronoun_markers = ("it", "this runtime", "that runtime", "它", "该环境", "当前环境", "这个环境")
    direct_clauses = []
    associated_clauses = []
    for item in environment_items:
        item_clauses = re.split(r"[,，;；!?。]+|\.(?![a-z0-9])", item.casefold().replace("’", "'"))
        for index, clause in enumerate(item_clauses):
            if not re.search(stack_pattern, clause):
                continue
            direct_clauses.append(clause)
            associated_clauses.append(clause)
            for following in item_clauses[index + 1:]:
                normalized = following.strip()
                normalized = re.sub(r"^(?:(?:but|however|yet)\b|但是|然而|不过|但)\s*", "", normalized)
                refers_to_stack = normalized.startswith(pronoun_markers)
                carries_unqualified_status = any(
                    normalized.startswith(marker)
                    for marker in (*unavailable_markers, *unknown_markers, "is not ", "isn't ", "isn’t ")
                )
                if not normalized or not (refers_to_stack or carries_unqualified_status):
                    break
                associated_clauses.append(following)
    return any(
        any(marker in clause for marker in available_markers) for clause in direct_clauses
    ) and not any(
        any(marker in clause for marker in (*unavailable_markers, *unknown_markers))
        for clause in associated_clauses
    )


def _explicit_dependency_indices(criteria: list[str], criterion_index: int) -> list[int]:
    criterion = criteria[criterion_index].casefold()
    suffix_markers = ("后", "之后", "完成后", "后再", "后才能", "after", "before", "then")
    prefix_markers = ("前置", "依赖于", "依赖", "基于", "once", "after", "depends on", "requires")
    matches = []
    for referenced_index, referenced_criterion in enumerate(criteria):
        if referenced_index == criterion_index:
            continue
        referenced = referenced_criterion.casefold()
        start = 0
        while True:
            start = criterion.find(referenced, start)
            if start < 0:
                break
            end = start + len(referenced)
            before = criterion[max(0, start - 24):start].rstrip(" \t\"'“‘《「『")
            after = criterion[end:end + 24].lstrip(" \t\"'”’》」』")
            if any(before.endswith(marker) for marker in prefix_markers) or any(
                after.startswith(marker) for marker in suffix_markers
            ):
                matches.append((start, end, referenced_index))
            start += 1
    return list(dict.fromkeys(
        index for start, end, index in matches
        if not any(
            other_index != index
            and other_start <= start and end <= other_end
            and (other_start < start or end < other_end)
            for other_start, other_end, other_index in matches
        )
    ))


def _criteria_in_dependency_order(criteria: list[str]) -> tuple[list[int], dict[int, list[int]]]:
    dependencies = {
        index: _explicit_dependency_indices(criteria, index)
        for index in range(len(criteria))
    }
    ordered: list[int] = []
    remaining = set(dependencies)
    while remaining:
        ready = next((index for index in range(len(criteria)) if index in remaining and all(
            prerequisite in ordered for prerequisite in dependencies[index]
        )), None)
        if ready is None:
            raise ValueError("confirmed criterion dependencies contain a cycle")
        ordered.append(ready)
        remaining.remove(ready)
    return ordered, dependencies


def _criterion_dependency_closure(index: int, dependencies: dict[int, list[int]]) -> set[int]:
    closure: set[int] = set()
    pending = [index]
    while pending:
        current = pending.pop()
        if current in closure:
            continue
        closure.add(current)
        pending.extend(dependencies[current])
    return closure


@dataclass(frozen=True)
class _ConfirmedOutcome:
    text: str
    children: tuple[_ConfirmedOutcome, ...] = ()

    @property
    def node_count(self) -> int:
        return 1 + sum(child.node_count for child in self.children)

    @property
    def height(self) -> int:
        return max((child.height + 1 for child in self.children), default=0)


def _explicit_outcome_parts(text: str) -> list[str]:
    """Split explicit lists, retaining quoted examples and nested groups intact."""
    brackets = {"(": ")", "（": "）", "[": "]", "【": "】", "{": "}"}
    quotes = {'"': '"', "“": "”", "‘": "’", "`": "`"}
    stack: list[str] = []
    quote = None
    parts = []
    start = 0
    for position, character in enumerate(text):
        if quote:
            if character == quote and (position == 0 or text[position - 1] != "\\"):
                quote = None
        elif character in quotes:
            quote = quotes[character]
        elif character in brackets:
            stack.append(brackets[character])
        elif stack and character == stack[-1]:
            stack.pop()
        elif not stack and character in ";；\n":
            parts.append(text[start:position].strip())
            if len(parts) >= MAX_FALLBACK_NODE_COUNT:
                raise ValueError("confirmed outcome list exceeds the fallback node limit")
            start = position + 1
    parts.append(text[start:].strip())
    # A malformed list is not enough evidence to invent sub-outcomes.
    return parts if not stack and not quote and all(parts) else [text]


def _confirmed_outcome(text: str, depth: int = 0, count: list[int] | None = None) -> _ConfirmedOutcome:
    if depth > 8:
        raise ValueError("confirmed outcome nesting exceeds the supported tree depth")
    count = [0] if count is None else count
    count[0] += 1
    if count[0] > MAX_FALLBACK_NODE_COUNT:
        raise ValueError("confirmed outcome decomposition exceeds the fallback node limit")
    parts = _explicit_outcome_parts(text)
    if len(parts) == 1:
        # Parenthesized, explicitly listed sub-outcomes can themselves contain groups.
        match = re.fullmatch(r"([^()（）]+)(?:\((.*)\)|（(.*)）)", text, flags=re.DOTALL)
        if match:
            parts = _explicit_outcome_parts(match.group(2) if match.group(2) is not None else match.group(3))
    children = tuple(_confirmed_outcome(part, depth + 1, count) for part in parts) if len(parts) > 1 else ()
    return _ConfirmedOutcome(text, children)


def _needs_model_decomposition(outcome: _ConfirmedOutcome) -> bool:
    if outcome.children:
        child_texts = [child.text for child in outcome.children]
        return any(
            _needs_model_decomposition(child) or _explicit_dependency_indices(child_texts, index)
            for index, child in enumerate(outcome.children)
        ) or any(re.search(
            r"先|然后|之后|后再|再|\b(?:once|after|before|then)\b",
            child.text, flags=re.IGNORECASE,
        ) for child in outcome.children)
    # Conjoined/broad outcomes lack explicit, independently checkable boundaries.
    return bool(re.search(
        r"并且|并(?:登录|注册|退出|创建|实现|支持|提供|显示|返回|提交|保存|读取|写入|校验|验证|启动|运行|查看|删除|编辑|搜索|转换|计算)|"
        r"以及|同时|、|[和与及]|\band\b|[,，][^，,]{0,15}(?:能够|可以|支持|显示|返回|创建|发布|加入|登录|注册|运行|提交|读取|写入|校验|验证|保存|退出|跳转|shows|returns|creates|supports)|"
        r"(?:实现|完成|构建|开发).*(?:完整系统|整套系统|完整应用|完整项目|整个模块)|"
        r"\b(?:complete|entire|full)\s+(?:system|application|project|module)\b",
        outcome.text, flags=re.IGNORECASE,
    ))


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
    criterion_order, all_dependencies = _criteria_in_dependency_order(criteria)
    outcomes = [_confirmed_outcome(criterion) for criterion in criteria]
    eligible = {index for index, outcome in enumerate(outcomes) if not _needs_model_decomposition(outcome)}
    short_candidates = [index for index in criterion_order if index in eligible and not all_dependencies[index]]
    if not short_candidates:
        raise ValueError("目前没有可安全生成的单项验收成果；请先将复合标准拆成可独立检查的子成果，或恢复规划模型后重试")
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
        "normal": "提供与目标相符的必要约定，关键实现由学生完成",
        "hard": "只提供必要上下文和验收标准",
        "challenge": "只提供目标、约束和自动验收入口",
    }[difficulty]
    personalized_guidance = (
        f"{guidance}；复用学生已具备能力：{known_skills}（经验：{experience_text}）；"
        f"先检查并复用现有素材：{starting_assets}。"
    )

    root_tasks = [
        task("明确本次最小成果", f"用自己的话写明本次要交付的成果：{goal}；交付物为：{artifact}，并记录对应说明作为验收依据"),
        task("整理可检查标准", f"记录已确认的检查项及明确先后关系：{criteria_text}；未确认的依赖不得自行添加，用清单供后续成果节点对照"),
        task("记录约束与已有基础", f"注明已确认技术栈：{required_stack}；环境：{environment_text}；排除范围：{excluded_scope}；能力：{known_skills}；经验：{experience_text}；素材：{starting_assets}"),
    ]
    root_title = "目标与验收"
    root_description = f"先确定交付物“{artifact}”及验收清单；填写周期“{duration_minutes} 分钟”作为节奏参考，为已确认标准安排成果节点。"

    stage_capacity = MAX_FALLBACK_NODE_COUNT
    environment_items = [item.strip() for item in constraints.environment if item.strip()]
    environment_ready = has_required_stack and all(
        _environment_confirms_stack(stack, environment_items) for stack in stack_items
    )
    setup_needed = (
        has_required_stack
        and not environment_ready
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

    node_specs[0] = ("goal", None, 0, "目标与验收", root_title, root_description, root_tasks)
    criterion_capacity = stage_capacity - len(node_specs)
    representable_indices: set[int] = set()
    representable_depths: dict[int, int] = {}
    for index in criterion_order:
        dependencies = all_dependencies[index]
        if index not in eligible or len(dependencies) > 1 or any(
            prerequisite not in representable_indices or outcomes[prerequisite].children
            for prerequisite in dependencies
        ):
            continue
        depth = representable_depths[dependencies[0]] + 1 if dependencies else shared_depth
        if depth + outcomes[index].height <= 8:
            representable_indices.add(index)
            representable_depths[index] = depth

    selected_indices: set[int] = set()
    for candidate_index in range(len(criteria)):
        if candidate_index not in representable_indices:
            continue
        closure = _criterion_dependency_closure(candidate_index, all_dependencies)
        if not closure <= representable_indices:
            continue
        additional = closure - selected_indices
        if sum(outcomes[index].node_count for index in selected_indices | additional) <= max(0, criterion_capacity):
            selected_indices.update(additional)
    if not selected_indices:
        raise ValueError("确认的成果因节点数量、深度或依赖结构限制无法安全表达；请减少目标数量或简化依赖关系")

    def append_outcome(outcome: _ConfirmedOutcome, key: str, parent: str, depth: int, context: str) -> None:
        criterion = outcome.text
        criterion_title = criterion if len(criterion) <= 125 else criterion[:124] + "…"
        if outcome.children:
            child_text = "；".join(child.text for child in outcome.children)
            tasks = [
                task("编写子成果验收清单", f"只按已确认内容逐项写出验收证据：{child_text}；将清单保存到“{artifact}”的实验记录中"),
                task("准备共用素材与检查入口", f"检查并记录可复用素材“{starting_assets}”；为各子成果准备检查入口或人工记录表，遵守排除范围“{excluded_scope}”"),
                task("保存起始状态与验收方案", f"针对“{criterion}”记录当前已有结果、待完成结果以及各项验证方式；本节点只验收准备产物，所有下级成果完成后才能确认整项标准达成", "manual"),
            ]
            node_specs.append((key, parent, depth, "子成果验收准备", f"准备验收：{criterion_title}",
                               f"产出验收清单、共用检查入口和起始记录；范围：{context}。本节点完成不代表下级成果已完成。", tasks))
            for position, child in enumerate(outcome.children, start=1):
                append_outcome(child, f"{key}-{position}", key, depth + 1, context)
        else:
            tasks = [
                task("明确验收证据", f"为标准“{criterion}”选择与交付物“{artifact}”匹配的证据，如文件内容、运行结果、测试结果或人工检查"),
                task("完成该项最小产出", f"先检查已确认素材“{starting_assets}”和能力“{known_skills}”是否已满足标准；已满足时复用并补证据，否则只完成标准“{criterion}”所需的最小改动，不添加未确认技术或功能；核对排除范围：{excluded_scope}"),
                task("检查并记录结果", f"针对标准“{criterion}”运行适用的测试、示例或人工检查；保存结果，失败时修正后重验", "test"),
            ]
            node_specs.append((key, parent, depth, "成果实现与验证", f"实现并验证：{criterion_title}",
                               f"独立完成并自验已确认成果：“{criterion}”；所属标准：{context}。", tasks))

    criterion_keys: dict[int, str] = {}
    criterion_depths: dict[int, int] = {}
    for zero_index in criterion_order:
        if zero_index not in selected_indices:
            continue
        index = zero_index + 1
        parent = shared_parent
        depth = shared_depth
        dependencies = all_dependencies[zero_index]
        if dependencies:
            dependency_index = dependencies[0]
            if dependency_index not in criterion_keys:
                continue
            parent = criterion_keys[dependency_index]
            depth = criterion_depths[dependency_index] + 1
            if depth > 8:
                continue
        key = f"criterion-{index}"
        criterion_keys[zero_index] = key
        criterion_depths[zero_index] = depth
        append_outcome(outcomes[zero_index], key, parent, depth, criteria[zero_index])

    included_indices = set(criterion_keys)
    covered_indices = included_indices
    omitted_criteria = [criteria[index] for index in range(len(criteria)) if index not in covered_indices]
    omitted_count = len(omitted_criteria)
    purpose = [goal]
    if omitted_count > 0:
        purpose.append(f"因节点数量、树深度、单父节点结构限制或需要进一步澄清而未覆盖 {omitted_count} 项：{'；'.join(omitted_criteria)}")
    else:
        purpose.append("每条确认的验收标准均有对应成果节点与验证步骤")

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
            acceptance_criteria=_criteria_for_tasks(guided_tasks),
        ))
    return ExperimentTree(title=title, purpose=purpose, nodes=nodes)


def _safe_fallback_tree(intent: ConfirmedIntent, duration_minutes: int, difficulty: str) -> ExperimentTree:
    try:
        return deterministic_tree(intent, duration_minutes, difficulty)
    except ValueError as error:
        raise ToolError(str(error)) from error


def _normalized_match_text(value: str) -> str:
    return re.sub(r"\s+", "", value).casefold()


def _node_plan_text(node: PlanNode) -> str:
    return " ".join([
        node.title,
        node.description,
        *(item.title for item in node.tasks),
        *(item.description for item in node.tasks),
    ])


def _declares_criterion_omitted(tree: ExperimentTree, criterion: str) -> bool:
    normalized_criterion = _normalized_match_text(criterion)
    omission_markers = (
        "未覆盖", "未完整覆盖", "未完成", "未纳入", "暂不包含", "留待", "不在本次范围",
        "not covered", "omitted", "deferred", "out of scope",
    )
    return any(
        normalized_criterion in _normalized_match_text(item)
        and any(marker in item.casefold() for marker in omission_markers)
        for item in tree.purpose
    )


def _validate_criterion_coverage(tree: ExperimentTree, intent: ConfirmedIntent) -> None:
    children_by_parent: dict[str, list[PlanNode]] = {}
    for node in tree.nodes:
        if node.parent_key is not None:
            children_by_parent.setdefault(node.parent_key, []).append(node)

    def has_recursive_outcome_coverage(
        outcome: _ConfirmedOutcome,
        parent_candidates: list[PlanNode],
    ) -> bool:
        if not outcome.children and not _needs_model_decomposition(outcome):
            return True

        for parent in parent_candidates:
            direct_children = children_by_parent.get(parent.key, [])
            if not direct_children:
                continue

            descendants: list[PlanNode] = []
            pending = list(direct_children)
            while pending:
                descendant = pending.pop()
                descendants.append(descendant)
                pending.extend(children_by_parent.get(descendant.key, []))

            if not outcome.children:
                if len(descendants) >= 2:
                    return True
                continue

            candidates_by_child = []
            for child in outcome.children:
                normalized_child = _normalized_match_text(child.text)
                candidates = [
                    node for node in descendants
                    if normalized_child in _normalized_match_text(_node_plan_text(node))
                    and (
                        (not child.children and not _needs_model_decomposition(child))
                        or bool(children_by_parent.get(node.key))
                    )
                    and has_recursive_outcome_coverage(child, [node])
                ]
                if not candidates:
                    break
                candidates_by_child.append(candidates)
            else:
                matched_children: dict[str, int] = {}

                def assign_child(index: int, visited: set[str]) -> bool:
                    for candidate in candidates_by_child[index]:
                        if candidate.key in visited:
                            continue
                        visited.add(candidate.key)
                        previous = matched_children.get(candidate.key)
                        if previous is None or assign_child(previous, visited):
                            matched_children[candidate.key] = index
                            return True
                    return False

                if all(assign_child(index, set()) for index in range(len(candidates_by_child))):
                    return True
        return False

    for criterion in dict.fromkeys(item.strip() for item in intent.intent.success_criteria if item.strip()):
        if _declares_criterion_omitted(tree, criterion):
            continue

        normalized_criterion = _normalized_match_text(criterion)
        matching_nodes = [
            node for node in tree.nodes
            if normalized_criterion in _normalized_match_text(_node_plan_text(node))
        ]
        if not matching_nodes:
            raise ValueError(f"generated plan silently omits confirmed success criterion: {criterion[:120]}")

        outcome = _confirmed_outcome(criterion)
        needs_decomposition = bool(outcome.children) or _needs_model_decomposition(outcome)
        if needs_decomposition:
            decomposed = [node for node in matching_nodes if children_by_parent.get(node.key)]
            if not decomposed:
                raise ValueError(f"composite success criterion remains an unsplit leaf: {criterion[:120]}")
            if not has_recursive_outcome_coverage(outcome, decomposed):
                raise ValueError(f"composite success criterion lacks independently traceable child outcomes: {criterion[:120]}")
            continue

        non_root_match = any(node.depth > 0 for node in matching_nodes)
        single_stage_action_match = len(tree.nodes) == 1 and any(
            normalized_criterion in _normalized_match_text(task_item.description)
            and any(verb in task_item.title for verb in (
                "实现", "完成", "创建", "构建", "编写", "执行", "运行", "验证", "检查", "产出", "提交", "演示",
            ))
            for task_item in tree.nodes[0].tasks
        )
        if not non_root_match and not single_stage_action_match:
            raise ValueError(f"confirmed criterion appears only in planning text, not an executable outcome node: {criterion[:120]}")


def _validate_model_tree(result: Any, duration_minutes: int, intent: ConfirmedIntent) -> ExperimentTree:
    tree = _ensure_node_acceptance_criteria(ExperimentTree.model_validate(result))
    _validate_criterion_coverage(tree, intent)
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
        return _safe_fallback_tree(intent, duration_minutes, difficulty)
    request_payload = {
        "confirmed_intent": intent.model_dump(mode="json"),
        "duration_minutes": duration_minutes,
        "difficulty": difficulty,
    }
    schema = ExperimentTree.model_json_schema()
    try:
        system_prompt = planner_system_prompt()
    except RuntimeError as error:
        logger.warning(
            "planner skill unavailable; using safe fallback (failure=%s)",
            type(error).__name__,
        )
        return _safe_fallback_tree(intent, duration_minutes, difficulty)
    try:
        result = await generate_json(system_prompt, request_payload, "PLANNER_MODEL", schema)
    except (ProviderError, httpx.HTTPError, json.JSONDecodeError) as error:
        if not (
            isinstance(error, ProviderError) and str(error) == "model returned invalid JSON"
            or isinstance(error, json.JSONDecodeError)
        ):
            logger.warning(
                "planner model request failed; using safe fallback (failure=%s)",
                type(error).__name__,
            )
            return _safe_fallback_tree(intent, duration_minutes, difficulty)
        result = {"_model_output_invalid": True}
    try:
        tree = _validate_model_tree(result, duration_minutes, intent)
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
            "instruction": "修复合同、验收标准覆盖与递归细分；每条成功标准须原文出现在可执行成果节点，预算不足时在purpose原文标注未覆盖。不改变 ConfirmedIntent。",
        }
        try:
            repaired = await generate_json(
                system_prompt + "\n当前请求是格式修复，必须返回完整合法实验树。",
                repair_payload,
                "PLANNER_MODEL",
                schema,
            )
        except (ProviderError, httpx.HTTPError, json.JSONDecodeError) as second_error:
            logger.warning(
                "planner model repair failed; using safe fallback (failure=%s)",
                type(second_error).__name__,
            )
            return _safe_fallback_tree(intent, duration_minutes, difficulty)
        try:
            tree = _validate_model_tree(repaired, duration_minutes, intent)
        except (ValidationError, ValueError) as second_error:
            logger.warning(
                "planner model output failed contract or budget validation twice; using safe fallback "
                "(failure=%s)",
                type(second_error).__name__,
            )
            tree = _safe_fallback_tree(intent, duration_minutes, difficulty)
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
