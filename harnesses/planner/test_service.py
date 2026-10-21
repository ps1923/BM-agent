import asyncio
import httpx
import pytest

from harnesses.common.contracts import ConfirmedIntent, CurrentState, ExperimentTree, IntentConstraints, IntentPreferences, StudentIntent
from harnesses.planner import service
from harnesses.planner.service import deterministic_tree
from harnesses.common.provider import ProviderError
from mcp.server.mcpserver.exceptions import ToolError


def sample_intent() -> ConfirmedIntent:
    return ConfirmedIntent(
        schema_version="0.1",
        intent_hash="a" * 64,
        confirmed_at="2026-09-11T00:00:00Z",
        intent=StudentIntent(
            raw_request="创建学习画布",
            observable_outcome="创建能够拖动节点并查看详情的学习画布",
            target_artifact="可运行的 Web 应用",
            current_state=CurrentState(),
            constraints=IntentConstraints(),
            preferences=IntentPreferences(),
            success_criteria=["可以拖动节点", "可以查看详情"],
        ),
    )


def test_planner_builds_dependency_flow_with_tasks():
    tree = deterministic_tree(sample_intent(), 1440, "normal")
    widths = {}
    for node in tree.nodes:
        widths[node.depth] = widths.get(node.depth, 0) + 1
        assert 3 <= len(node.tasks) <= 7
        assert node.unlock_rule == "all_prerequisites_completed"
        assert node.completion_rule == "all_tasks_completed"
        assert node.prerequisite_keys == ([] if node.parent_key is None else [node.parent_key])
    assert widths == {0: 1, 1: 2}


def test_deterministic_fallback_uses_confirmed_goal_instead_of_fixed_web_template():
    intent = sample_intent()
    intent.intent.raw_request = "创建一个 Python 命令行温度转换器"
    intent.intent.observable_outcome = "实现摄氏度与华氏度之间的命令行温度转换"
    intent.intent.target_artifact = "Python 命令行程序"
    intent.intent.constraints.required_stack = ["Python"]
    intent.intent.constraints.excluded_scope = ["网页界面"]
    intent.intent.success_criteria = ["摄氏转华氏结果正确", "华氏转摄氏结果正确"]
    intent.intent.current_state.known_skills = ["Python 基础"]
    intent.intent.current_state.starting_assets = ["已有项目目录"]

    tree = deterministic_tree(intent, 240, "normal")
    rendered = " ".join(
        f"{node.stage} {node.title} {node.description} " + " ".join(
            f"{item.title} {item.description}" for item in node.tasks
        )
        for node in tree.nodes
    )

    assert "温度转换" in rendered
    assert "Python" in rendered
    assert "Python 基础" in rendered
    assert "已有项目目录" in rendered
    assert "网页界面" in rendered
    assert "创建可拖动气泡" not in rendered
    assert "MySQL" not in rendered
    assert all(3 <= len(node.tasks) <= 7 for node in tree.nodes)
    criteria_nodes = [node for node in tree.nodes if node.key.startswith("criterion-")]
    assert [node.key for node in criteria_nodes] == ["criterion-1", "criterion-2"]
    assert all(node.parent_key == "setup" for node in criteria_nodes)
    assert all(
        criterion in " ".join(task.description for task in node.tasks)
        for criterion, node in zip(intent.intent.success_criteria, criteria_nodes)
    )


def test_ordering_word_in_one_criterion_does_not_chain_unrelated_criteria():
    intent = sample_intent()
    intent.intent.success_criteria = [
        "配置环境后程序能够启动",
        "输入有效数据时返回正确结果",
        "无效输入能够显示错误提示",
    ]

    tree = deterministic_tree(intent, 120, "normal")

    criteria_nodes = [node for node in tree.nodes if node.key.startswith("criterion-")]
    assert [node.parent_key for node in criteria_nodes] == ["goal", "goal", "goal"]
    assert [node.depth for node in criteria_nodes] == [1, 1, 1]
    assert [node.prerequisite_keys for node in criteria_nodes] == [["goal"], ["goal"], ["goal"]]


def test_explicit_reference_to_previous_criterion_preserves_only_that_dependency():
    intent = sample_intent()
    intent.intent.success_criteria = [
        "配置 Python 环境",
        "完成配置 Python 环境后程序能够启动",
        "无效输入能够显示错误提示",
    ]

    tree = deterministic_tree(intent, 120, "normal")

    criteria_nodes = [node for node in tree.nodes if node.key.startswith("criterion-")]
    assert [(node.parent_key, node.depth) for node in criteria_nodes] == [
        ("goal", 1),
        ("criterion-1", 2),
        ("goal", 1),
    ]


def test_quoted_explicit_prerequisite_is_preserved():
    intent = sample_intent()
    intent.intent.success_criteria = [
        "配置 Python 环境",
        "完成“配置 Python 环境”后程序能够启动",
    ]

    tree = deterministic_tree(intent, 90, "normal")

    criteria_nodes = [node for node in tree.nodes if node.key.startswith("criterion-")]
    assert [(node.parent_key, node.depth) for node in criteria_nodes] == [
        ("goal", 1),
        ("criterion-1", 2),
    ]


def test_after_phrase_before_prerequisite_is_preserved():
    intent = sample_intent()
    intent.intent.success_criteria = [
        "Launch the application after Install Python",
        "Install Python",
    ]

    tree = deterministic_tree(intent, 90, "normal")

    criteria_nodes = [node for node in tree.nodes if node.key.startswith("criterion-")]
    assert [(node.key, node.parent_key, node.depth) for node in criteria_nodes] == [
        ("criterion-2", "goal", 1),
        ("criterion-1", "criterion-2", 2),
    ]


def test_nested_confirmed_prerequisite_mentions_are_both_preserved():
    intent = sample_intent()
    intent.intent.success_criteria = [
        "环境配置",
        "环境配置文件已创建",
        "环境配置后，环境配置文件已创建后程序启动",
    ]

    tree = deterministic_tree(intent, 90, "normal")

    criteria_nodes = [node for node in tree.nodes if node.key.startswith("criterion-")]
    assert [node.key for node in criteria_nodes] == ["criterion-1", "criterion-2"]
    assert intent.intent.success_criteria[2] in " ".join(tree.purpose)


def test_repeated_mention_of_one_prerequisite_does_not_look_like_a_join():
    intent = sample_intent()
    intent.intent.success_criteria = [
        "配置环境",
        "配置环境后确认配置环境完成后检查",
    ]

    tree = deterministic_tree(intent, 90, "normal")

    criteria_nodes = [node for node in tree.nodes if node.key.startswith("criterion-")]
    assert [(node.key, node.parent_key, node.depth) for node in criteria_nodes] == [
        ("criterion-1", "goal", 1),
        ("criterion-2", "criterion-1", 2),
    ]


def test_explicit_nested_outcomes_become_multilevel_learning_nodes():
    intent = sample_intent()
    intent.intent.success_criteria = [
        "输入校验（必填校验（邮箱为空有提示；密码为空有提示）；格式校验（邮箱格式错误有提示；密码过短有提示））",
    ]

    tree = deterministic_tree(intent, 180, "normal")

    by_key = {node.key: node for node in tree.nodes}
    assert len(tree.nodes) == 8
    assert by_key["criterion-1-1-1"].parent_key == "criterion-1-1"
    assert by_key["criterion-1-1-1"].depth == 3
    assert "邮箱为空有提示" in by_key["criterion-1-1-1"].title
    assert "密码过短有提示" in by_key["criterion-1-2-2"].title
    assert "验收清单" in by_key["criterion-1"].tasks[0].title
    assert "本节点完成不代表下级成果已完成" in by_key["criterion-1"].description
    assert all(3 <= len(node.tasks) <= 7 for node in tree.nodes)
    assert sum(node.estimated_minutes for node in tree.nodes) <= 180
    assert tree == deterministic_tree(intent, 180, "normal")


def test_recursive_outcome_parser_rejects_lists_above_tree_node_contract():
    intent = sample_intent()
    intent.intent.success_criteria = ["；".join(f"第 {index} 项可检查" for index in range(81))]

    with pytest.raises(ValueError, match="fallback node limit"):
        deterministic_tree(intent, 1_440, "normal")


def test_compound_outcome_cost_is_included_before_selecting_criteria():
    intent = sample_intent()
    intent.intent.success_criteria = ["邮箱为空有提示；密码为空有提示", "帮助命令显示用法"]

    tree = deterministic_tree(intent, 45, "normal")

    assert [node.key for node in tree.nodes] == ["goal", "criterion-2"]
    assert intent.intent.success_criteria[0] in " ".join(tree.purpose)
    assert "未覆盖 1 项" in " ".join(tree.purpose)


def test_short_compound_outcome_covers_only_one_confirmed_leaf():
    intent = sample_intent()
    intent.intent.success_criteria = ["邮箱为空有提示；密码为空有提示"]

    tree = deterministic_tree(intent, 20, "normal")

    assert len(tree.nodes) == 1
    assert "邮箱为空有提示" in tree.nodes[0].tasks[1].description
    assert "密码为空有提示" not in tree.nodes[0].tasks[1].description
    assert "未完整覆盖 1 项" in " ".join(tree.purpose)


@pytest.mark.parametrize("criterion", [
    "实现完整系统",
    "可以注册并且可以登录",
    "用户可以注册并登录",
    "支持注册和登录",
    "支持注册、登录、课程发布",
    "管理员可以创建课程，学生可以加入课程",
    "环境可运行；环境可运行后接口启动成功",
    "创建数据库表；然后接入后端接口",
])
def test_unsafely_decomposable_criteria_fail_clearly(criterion):
    intent = sample_intent()
    intent.intent.success_criteria = [criterion]

    with pytest.raises(ValueError, match="复合标准"):
        deterministic_tree(intent, 180, "normal")


def test_mcp_tool_exposes_fallback_decomposition_error_to_the_caller(monkeypatch):
    monkeypatch.setenv("HARNESS_PROVIDER", "deterministic")
    intent = sample_intent()
    intent.intent.success_criteria = ["实现完整系统"]

    with pytest.raises(ToolError, match="复合标准"):
        asyncio.run(service.generate_experiment_tree(
            intent.model_dump(mode="json"), 180, "normal"))


def test_quoted_semicolon_is_not_a_list_of_outcomes():
    intent = sample_intent()
    intent.intent.success_criteria = ['输入 "a;b" 时原样输出']

    tree = deterministic_tree(intent, 60, "normal")

    assert [node.key for node in tree.nodes] == ["goal", "criterion-1"]


def test_dependency_on_all_expanded_leaves_does_not_unlock_from_their_preparation_node():
    intent = sample_intent()
    composite = "输入校验（邮箱为空有提示；密码为空有提示）"
    intent.intent.success_criteria = [composite, f"完成{composite}后提交表单"]

    tree = deterministic_tree(intent, 180, "normal")

    assert "criterion-1-2" in {node.key for node in tree.nodes}
    assert "criterion-2" not in {node.key for node in tree.nodes}
    assert intent.intent.success_criteria[1] in " ".join(tree.purpose)


def test_explicit_dependency_reorders_criteria_when_prerequisite_is_listed_later():
    intent = sample_intent()
    intent.intent.success_criteria = [
        "程序能够启动，依赖配置 Python 环境",
        "配置 Python 环境",
        "无效输入能够显示错误提示",
    ]

    tree = deterministic_tree(intent, 120, "normal")

    criteria_nodes = [node for node in tree.nodes if node.key.startswith("criterion-")]
    assert [(node.key, node.parent_key, node.depth) for node in criteria_nodes] == [
        ("criterion-2", "goal", 1),
        ("criterion-1", "criterion-2", 2),
        ("criterion-3", "goal", 1),
    ]


def test_budget_omits_a_multi_prerequisite_result_instead_of_unlocking_it_early():
    intent = sample_intent()
    intent.intent.success_criteria = [
        "配置环境后，安装数据库后才能启动程序",
        "配置环境",
        "安装数据库",
    ]

    tree = deterministic_tree(intent, 45, "normal")

    assert [node.key for node in tree.nodes if node.key.startswith("criterion-")] == [
        "criterion-2", "criterion-3",
    ]
    assert intent.intent.success_criteria[0] in " ".join(tree.purpose)


def test_multiple_prerequisites_are_not_serialized_into_unconfirmed_order():
    intent = sample_intent()
    intent.intent.success_criteria = [
        "配置环境后，安装数据库后才能启动程序",
        "配置环境",
        "安装数据库",
    ]

    tree = deterministic_tree(intent, 60, "normal")

    criteria_nodes = [node for node in tree.nodes if node.key.startswith("criterion-")]
    assert [(node.key, node.parent_key, node.depth) for node in criteria_nodes] == [
        ("criterion-2", "goal", 1),
        ("criterion-3", "goal", 1),
    ]
    rendered_purpose = " ".join(tree.purpose)
    assert intent.intent.success_criteria[0] in rendered_purpose
    assert "未覆盖 1 项" in rendered_purpose


def test_over_depth_dependency_chain_omits_only_unrepresentable_criteria():
    intent = sample_intent()
    criteria = ["步骤 1 完成"]
    for index in range(2, 11):
        criteria.append(f"{criteria[-1]} 后步骤 {index} 完成")
    intent.intent.success_criteria = criteria

    tree = deterministic_tree(intent, 600, "normal")

    criteria_nodes = [node for node in tree.nodes if node.key.startswith("criterion-")]
    assert max(node.depth for node in criteria_nodes) <= 8
    assert len(criteria_nodes) == 8
    rendered_purpose = " ".join(tree.purpose)
    assert criteria[8] in rendered_purpose
    assert criteria[9] in rendered_purpose
    assert "未覆盖 2 项" in rendered_purpose


def test_depth_limited_chain_does_not_consume_capacity_for_independent_criterion():
    intent = sample_intent()
    criteria = ["步骤 1 完成"]
    for index in range(2, 10):
        criteria.append(f"{criteria[-1]} 后步骤 {index} 完成")
    criteria.append("独立成果能够通过检查")
    intent.intent.success_criteria = criteria

    tree = deterministic_tree(intent, 150, "normal")

    criteria_nodes = [node for node in tree.nodes if node.key.startswith("criterion-")]
    assert "criterion-10" in {node.key for node in criteria_nodes}
    assert criteria[8] in " ".join(tree.purpose)
    assert "未覆盖 1 项" in " ".join(tree.purpose)


def test_fallback_collapses_stages_for_short_durations_and_preserves_end_to_end_work():
    expected_node_counts = {15: 1, 29: 1, 30: 2, 44: 2, 45: 3, 59: 3, 60: 3}

    for minutes, expected_count in expected_node_counts.items():
        tree = deterministic_tree(sample_intent(), minutes, "normal")

        assert len(tree.nodes) == expected_count
        assert sum(node.estimated_minutes for node in tree.nodes) == minutes
        assert tree.nodes[0].parent_key is None
        assert all(node.estimated_minutes >= 15 for node in tree.nodes)
        assert all(3 <= len(node.tasks) <= 7 for node in tree.nodes)

    with pytest.raises(ValueError, match="at least 15"):
        deterministic_tree(sample_intent(), 14, "normal")


def test_short_fallback_selects_the_first_available_confirmed_prerequisite():
    intent = sample_intent()
    intent.intent.success_criteria = [
        "程序能够启动，依赖配置 Python 环境",
        "配置 Python 环境",
    ]

    short_tree = deterministic_tree(intent, 20, "normal")
    tight_tree = deterministic_tree(intent, 30, "normal")

    assert "配置 Python 环境" in " ".join(task.description for task in short_tree.nodes[0].tasks)
    assert intent.intent.success_criteria[0] in " ".join(short_tree.purpose)
    assert [node.key for node in tight_tree.nodes] == ["goal", "criterion-2"]
    assert tight_tree.nodes[1].parent_key == "goal"
    assert intent.intent.success_criteria[0] in " ".join(tight_tree.purpose)


def test_fallback_prepares_stack_only_when_no_existing_starting_point_is_confirmed():
    intent = sample_intent()
    intent.intent.constraints.required_stack = ["Python"]

    unprepared = deterministic_tree(intent, 60, "normal")
    intent.intent.current_state.starting_assets = ["existing project"]
    assets_only = deterministic_tree(intent, 60, "normal")
    intent.intent.constraints.environment = ["Python runtime is installed"]
    prepared = deterministic_tree(intent, 60, "normal")

    assert any(node.key == "setup" for node in unprepared.nodes)
    assert any(node.key == "setup" for node in assets_only.nodes)
    assert all(node.key != "setup" for node in prepared.nodes)
    assert len(unprepared.nodes) == 4
    assert len(prepared.nodes) == 3


def test_short_unprepared_stack_is_preserved_in_plan_when_a_setup_node_will_not_fit():
    intent = sample_intent()
    intent.intent.constraints.required_stack = ["Python"]
    intent.intent.constraints.excluded_scope = ["网页界面"]

    for minutes in (15, 29, 30, 44):
        tree = deterministic_tree(intent, minutes, "normal")
        rendered = " ".join(
            f"{node.description} " + " ".join(task.description for task in node.tasks)
            for node in tree.nodes
        )

        assert "Python" in rendered
        assert "网页界面" in rendered
        assert len(tree.nodes[0].tasks) == 3
        assert sum(node.estimated_minutes for node in tree.nodes) <= minutes
        if minutes in (30, 44):
            assert tree.nodes[0].tasks[0].title == "检查现有环境与素材"


def test_general_environment_note_does_not_claim_required_stack_is_ready():
    intent = sample_intent()
    intent.intent.constraints.required_stack = ["Python"]
    intent.intent.constraints.environment = ["Linux 学校机房"]

    tree = deterministic_tree(intent, 90, "normal")

    assert any(node.key == "setup" for node in tree.nodes)


def test_explicitly_unavailable_required_stack_does_not_count_as_ready():
    intent = sample_intent()
    intent.intent.constraints.required_stack = ["Python"]
    intent.intent.constraints.environment = ["Python is not installed"]

    tree = deterministic_tree(intent, 90, "normal")

    assert any(node.key == "setup" for node in tree.nodes)


@pytest.mark.parametrize("environment", [
    "Python is not present",
    "Python is installed but not configured",
])
def test_partially_or_negatively_confirmed_stack_does_not_skip_setup(environment):
    intent = sample_intent()
    intent.intent.constraints.required_stack = ["Python"]
    intent.intent.constraints.environment = [environment]

    tree = deterministic_tree(intent, 90, "normal")

    assert any(node.key == "setup" for node in tree.nodes)


@pytest.mark.parametrize("environment", [
    "Python is not present",
    "Python is installed but not configured",
    "Python runtime isn't available",
    "Python is not currently installed",
    "Python isn't currently installed",
    "Python isn’t currently installed",
])
def test_unavailable_or_partially_configured_stack_does_not_skip_setup(environment):
    intent = sample_intent()
    intent.intent.constraints.required_stack = ["Python"]
    intent.intent.constraints.environment = [environment]

    tree = deterministic_tree(intent, 90, "normal")

    assert any(node.key == "setup" for node in tree.nodes)


def test_following_pronoun_negation_keeps_stack_unready():
    intent = sample_intent()
    intent.intent.constraints.required_stack = ["Python"]
    intent.intent.constraints.environment = ["Python is installed; it is not configured"]

    tree = deterministic_tree(intent, 90, "normal")

    assert any(node.key == "setup" for node in tree.nodes)


@pytest.mark.parametrize("environment", [
    "Python is installed, but it is not configured",
    "Python 已安装，但尚未配置",
])
def test_contrast_clause_negation_keeps_stack_unready(environment):
    intent = sample_intent()
    intent.intent.constraints.required_stack = ["Python"]
    intent.intent.constraints.environment = [environment]

    tree = deterministic_tree(intent, 90, "normal")

    assert any(node.key == "setup" for node in tree.nodes)


def test_stack_substring_inside_another_technology_does_not_count_as_ready():
    intent = sample_intent()
    intent.intent.constraints.required_stack = ["C"]
    intent.intent.constraints.environment = ["Visual Studio Code installed"]

    tree = deterministic_tree(intent, 90, "normal")

    assert any(node.key == "setup" for node in tree.nodes)


def test_unavailable_note_for_another_stack_does_not_block_confirmed_stack():
    intent = sample_intent()
    intent.intent.constraints.required_stack = ["Node.js"]
    intent.intent.constraints.environment = ["Node.js is installed; Python is not installed"]

    tree = deterministic_tree(intent, 90, "normal")

    assert all(node.key != "setup" for node in tree.nodes)


def test_unknown_environment_status_does_not_count_as_confirmed_ready():
    intent = sample_intent()
    intent.intent.constraints.required_stack = ["Python"]
    intent.intent.constraints.environment = ["Python runtime status is unknown"]

    tree = deterministic_tree(intent, 90, "normal")

    assert any(node.key == "setup" for node in tree.nodes)


def test_confirmed_stack_readiness_skips_separate_setup_node():
    intent = sample_intent()
    intent.intent.constraints.required_stack = ["Python"]
    intent.intent.constraints.environment = ["Python runtime is installed"]

    tree = deterministic_tree(intent, 90, "normal")

    assert all(node.key != "setup" for node in tree.nodes)


def test_fallback_distributes_remainder_minutes_without_exceeding_budget():
    tree = deterministic_tree(sample_intent(), 61, "normal")

    assert [node.estimated_minutes for node in tree.nodes] == [21, 20, 20]
    assert sum(node.estimated_minutes for node in tree.nodes) == 61


def test_fallback_caps_each_node_on_very_long_durations():
    duration = 3650 * 1440
    tree = deterministic_tree(sample_intent(), duration, "normal")

    assert all(15 <= node.estimated_minutes <= 1440 for node in tree.nodes)
    assert sum(node.estimated_minutes for node in tree.nodes) <= duration


def test_fallback_rejects_intents_without_a_verifiable_outcome():
    intent = sample_intent()
    intent.intent.observable_outcome = "   "
    intent.intent.target_artifact = "   "
    intent.intent.success_criteria = ["  "]

    with pytest.raises(ValueError, match="observable outcome and non-empty success criteria"):
        deterministic_tree(intent, 60, "normal")


def test_model_plan_must_fit_exact_duration_budget():
    plan = deterministic_tree(sample_intent(), 1_440, "normal").model_dump(mode="json")
    plan["nodes"][0]["estimated_minutes"] += 1

    with pytest.raises(ValueError, match="exceeds duration budget"):
        service._validate_model_tree(plan, 1_440, sample_intent())


def test_model_plan_rejects_a_criterion_silently_omitted_from_nodes_and_purpose():
    intent = sample_intent()
    intent.intent.success_criteria = ["登录页面显示明确错误提示"]
    plan = deterministic_tree(intent, 90, "normal").model_dump(mode="json")
    criterion = intent.intent.success_criteria[0]

    def remove_criterion(value):
        if isinstance(value, dict):
            for key, item in value.items():
                value[key] = remove_criterion(item)
            return value
        if isinstance(value, list):
            return [remove_criterion(item) for item in value]
        if isinstance(value, str):
            return value.replace(criterion, "")
        return value

    remove_criterion(plan)

    with pytest.raises(ValueError, match="silently omits confirmed success criterion"):
        service._validate_model_tree(plan, 90, intent)


def test_model_plan_cannot_count_root_planning_checklist_as_a_completed_criterion():
    intent = sample_intent()
    intent.intent.success_criteria = ["登录页面显示明确错误提示"]
    plan = deterministic_tree(intent, 30, "normal").model_dump(mode="json")
    plan["nodes"] = plan["nodes"][:1]
    plan["nodes"][0]["estimated_minutes"] = 30
    plan["nodes"][0]["title"] = "实验规划"
    plan["nodes"][0]["description"] = "规划本次实验"
    plan["nodes"][0]["tasks"] = [
        {"title": "列出验收标准", "description": "登录页面显示明确错误提示", "validation_type": "manual", "validation_config": {}},
        {"title": "整理实验范围", "description": "整理实验范围", "validation_type": "manual", "validation_config": {}},
        {"title": "确认实验条件", "description": "确认实验条件", "validation_type": "manual", "validation_config": {}},
    ]

    with pytest.raises(ValueError, match="appears only in planning text"):
        service._validate_model_tree(plan, 30, intent)


def test_model_plan_must_split_composite_success_criteria_into_child_nodes():
    intent = sample_intent()
    intent.intent.success_criteria = ["用户可以注册并登录"]
    plan = {
        "schema_version": "0.1",
        "title": "用户系统实验",
        "purpose": ["完成用户可以注册并登录"],
        "nodes": [
            {
                "key": "goal", "parent_key": None, "prerequisite_keys": [],
                "unlock_rule": "all_prerequisites_completed", "completion_rule": "all_tasks_completed",
                "depth": 0, "stage": "目标与验收", "title": "用户系统实验",
                "description": "规划实验成果", "estimated_minutes": 15,
                "tasks": [
                    {"title": "明确目标", "description": "明确实验目标", "validation_type": "manual", "validation_config": {}},
                    {"title": "列出成果", "description": "列出用户可以注册并登录", "validation_type": "manual", "validation_config": {}},
                    {"title": "检查范围", "description": "检查本次实验范围", "validation_type": "manual", "validation_config": {}},
                ],
            },
            {
                "key": "account-flow", "parent_key": "goal", "prerequisite_keys": ["goal"],
                "unlock_rule": "all_prerequisites_completed", "completion_rule": "all_tasks_completed",
                "depth": 1, "stage": "用户功能", "title": "用户可以注册并登录",
                "description": "完成注册并登录", "estimated_minutes": 30,
                "tasks": [
                    {"title": "实现功能", "description": "完成用户可以注册并登录", "validation_type": "manual", "validation_config": {}},
                    {"title": "编写测试", "description": "检查功能行为", "validation_type": "test", "validation_config": {}},
                    {"title": "运行测试", "description": "保存测试结果", "validation_type": "command", "validation_config": {}},
                ],
            },
        ],
    }

    with pytest.raises(ValueError, match="lacks independently traceable child outcomes"):
        service._validate_model_tree(plan, 60, intent)


def test_model_plan_must_recursively_split_nested_composite_outcomes():
    intent = sample_intent()
    criterion = "注册（邮箱为空提示；密码过短提示）；登录后显示欢迎页"
    intent.intent.success_criteria = [criterion]

    def node(key, parent_key, depth, title):
        return {
            "key": key,
            "parent_key": parent_key,
            "prerequisite_keys": [] if parent_key is None else [parent_key],
            "unlock_rule": "all_prerequisites_completed",
            "completion_rule": "all_tasks_completed",
            "depth": depth,
            "stage": "账户功能",
            "title": title,
            "description": "完成并验证本阶段成果",
            "estimated_minutes": 15,
            "tasks": [
                {"title": "实现功能", "description": "实现并记录对应成果", "validation_type": "manual", "validation_config": {}},
                {"title": "编写测试", "description": "编写并运行相关测试", "validation_type": "test", "validation_config": {}},
                {"title": "验证结果", "description": "保存测试或运行证据", "validation_type": "command", "validation_config": {}},
            ],
        }

    outcome = service._confirmed_outcome(criterion)
    plan = {
        "schema_version": "0.1",
        "title": "账户实验",
        "purpose": ["实现账户功能"],
        "nodes": [
            node("goal", None, 0, "账户实验"),
            node("account-flow", "goal", 1, criterion),
            node("registration", "account-flow", 2, outcome.children[0].text),
            node("login", "account-flow", 2, outcome.children[1].text),
        ],
    }

    with pytest.raises(ValueError, match="lacks independently traceable child outcomes"):
        service._validate_model_tree(plan, 60, intent)

    plan["nodes"].extend([
        node("empty-email", "registration", 3, outcome.children[0].children[0].text),
        node("short-password", "registration", 3, outcome.children[0].children[1].text),
    ])
    tree = service._validate_model_tree(plan, 90, intent)

    assert tree.nodes[-2].parent_key == tree.nodes[-1].parent_key == "registration"


def test_model_plan_explicitly_lists_uncovered_criterion_in_purpose():
    intent = sample_intent()
    intent.intent.success_criteria = ["登录页面显示明确错误提示"]
    plan = deterministic_tree(intent, 90, "normal").model_dump(mode="json")
    criterion = intent.intent.success_criteria[0]
    plan["nodes"] = plan["nodes"][:1]
    plan["nodes"][0]["estimated_minutes"] = 90
    plan["purpose"] = [f"周期受限，未覆盖：{criterion}"]

    tree = service._validate_model_tree(plan, 90, intent)

    assert len(tree.nodes) == 1
    assert criterion in tree.purpose[0]


def test_planner_prompt_loads_recursive_learner_centered_skill():
    prompt = service.planner_system_prompt()

    assert "继续递归拆成子节点" in prompt
    assert "3.1.1" in prompt
    assert "父节点自身也必须有真实的 3–7 项任务" in prompt
    assert "当前合同只支持单父节点和单一父依赖" in prompt
    assert "不得静默漏项" in prompt
    assert "明确列出的子成果应分别落入不同后代节点" in prompt
    assert "不设统一的最低层级" in prompt
    assert "不为凑出 `3.1.1` 人工插入目录节点" in prompt


def test_planner_rejects_empty_skill_content(monkeypatch, tmp_path):
    skill_path = tmp_path / "node-planning.md"
    skill_path.write_text("  \n", encoding="utf-8")
    monkeypatch.setattr(service, "PLANNER_SKILL_PATH", skill_path)

    with pytest.raises(RuntimeError, match="skill is empty"):
        service.load_node_planning_skill()


def test_model_plan_accepts_multilevel_learning_nodes(monkeypatch):
    intent = sample_intent()
    intent.intent.success_criteria = [
        "输入缺少必填字段时拒绝请求",
        "字段格式错误时显示提示",
    ]

    model_prompt = None

    def plan_node(key, parent_key, depth, title):
        return {
            "key": key,
            "parent_key": parent_key,
            "prerequisite_keys": [] if parent_key is None else [parent_key],
            "unlock_rule": "all_prerequisites_completed",
            "completion_rule": "all_tasks_completed",
            "depth": depth,
            "stage": "接口实现",
            "title": title,
            "description": "实现一个可运行、可验证的小阶段产出",
            "estimated_minutes": 30,
            "tasks": [
                {"title": "实现字段校验", "description": "校验输入对象中的必填字段", "validation_type": "manual", "validation_config": {}},
                {"title": "编写校验测试", "description": "为有效和无效输入编写测试", "validation_type": "test", "validation_config": {}},
                {"title": "运行测试验证", "description": "运行测试并确认预期结果", "validation_type": "command", "validation_config": {}},
            ],
        }

    nested_tree = {
        "schema_version": "0.1",
        "title": "接口请求校验实验",
        "purpose": ["实现并验证请求字段校验"],
        "nodes": [
            plan_node("api-stage", None, 0, "3 实现核心接口"),
            plan_node("request-validation", "api-stage", 1, "3.1 实现请求校验"),
            plan_node("required-fields", "request-validation", 2, "3.1.1 输入缺少必填字段时拒绝请求"),
            plan_node("field-format", "request-validation", 2, "3.1.2 字段格式错误时显示提示"),
        ],
    }

    async def nested_model_output(system, *_args, **_kwargs):
        nonlocal model_prompt
        model_prompt = system
        return nested_tree

    monkeypatch.setenv("HARNESS_PROVIDER", "openai-compatible")
    monkeypatch.setattr(service, "generate_json", nested_model_output)

    result = asyncio.run(service.generate_experiment_tree(
        intent.model_dump(mode="json"), 1_440, "normal"))

    assert model_prompt is not None and "3.1.1" in model_prompt
    assert [(node.depth, node.title) for node in result.nodes] == [
        (0, "3 实现核心接口"),
        (1, "3.1 实现请求校验"),
        (2, "3.1.1 输入缺少必填字段时拒绝请求"),
        (2, "3.1.2 字段格式错误时显示提示"),
    ]
    assert result.nodes[2].parent_key == result.nodes[3].parent_key == "request-validation"


def test_planner_input_contract_has_no_conversation_history():
    fields = ConfirmedIntent.model_fields
    assert "messages" not in fields
    assert "conversation" not in fields


def test_contract_rejects_missing_parent_without_leaking_key_error():
    payload = deterministic_tree(sample_intent(), 1440, "normal").model_dump(mode="json")
    payload["nodes"][1]["parent_key"] = "missing"
    payload["nodes"][1]["prerequisite_keys"] = ["missing"]

    with pytest.raises(ValueError, match="invalid parent"):
        ExperimentTree.model_validate(payload)


def test_long_duration_is_capped_and_difficulty_changes_guidance():
    beginner = deterministic_tree(sample_intent(), 3650 * 1440, "beginner")
    challenge = deterministic_tree(sample_intent(), 3650 * 1440, "challenge")
    assert all(node.estimated_minutes <= 1440 for node in beginner.nodes)
    assert beginner.nodes[0].description != challenge.nodes[0].description
    assert beginner.nodes[0].tasks[0].description != challenge.nodes[0].tasks[0].description


def test_planner_fallback_uses_confirmed_stack_without_inventing_stack_specific_stages():
    intent = sample_intent()
    intent.intent.raw_request = "用 HTML、Python 和 MySQL 完成可运行的数据页面"
    intent.intent.constraints.required_stack.extend(["HTML", "Python", "MySQL"])
    intent.intent.success_criteria.append("完成验收并记录结果")
    tree = deterministic_tree(intent, 1440, "normal")

    assert len(tree.nodes) == 5
    assert tree.nodes[0].key == "goal"
    assert tree.nodes[1].key == "setup"
    assert all(node.parent_key == "setup" for node in tree.nodes[2:])
    rendered = " ".join(
        f"{node.description} " + " ".join(task.description for task in node.tasks)
        for node in tree.nodes
    )
    assert all(stack in rendered for stack in ("HTML", "Python", "MySQL"))
    assert "MySQL 真实读取" not in rendered


def test_planner_returns_safe_tree_after_two_invalid_model_outputs(monkeypatch):
    calls = 0

    async def invalid_model_output(*_args, **_kwargs):
        nonlocal calls
        calls += 1
        return {"nodes": []}

    monkeypatch.setenv("HARNESS_PROVIDER", "openai-compatible")
    monkeypatch.setattr(service, "generate_json", invalid_model_output)

    result = asyncio.run(service.generate_experiment_tree(
        sample_intent().model_dump(mode="json"), 1_440, "normal"))

    assert calls == 2
    assert len(result.nodes) == 3
    assert {node.depth for node in result.nodes} == {0, 1}


def test_planner_recovers_from_budget_violation_after_repair_fails(monkeypatch):
    calls = 0
    over_budget = deterministic_tree(sample_intent(), 1_440, "normal").model_dump(mode="json")
    for node in over_budget["nodes"]:
        node["estimated_minutes"] = 1_440

    async def outputs(*_args, **_kwargs):
        nonlocal calls
        calls += 1
        return over_budget if calls == 1 else {"invalid": True}

    monkeypatch.setenv("HARNESS_PROVIDER", "openai-compatible")
    monkeypatch.setattr(service, "generate_json", outputs)

    result = asyncio.run(service.generate_experiment_tree(
        sample_intent().model_dump(mode="json"), 1_440, "normal"))

    assert calls == 2
    assert sum(node.estimated_minutes for node in result.nodes) <= 1_440


def test_planner_repairs_output_that_exceeds_budget_within_old_tolerance(monkeypatch):
    calls = 0
    over_budget = deterministic_tree(sample_intent(), 1_440, "normal").model_dump(mode="json")
    over_budget["nodes"][0]["estimated_minutes"] += 30

    async def outputs(*_args, **_kwargs):
        nonlocal calls
        calls += 1
        return over_budget if calls == 1 else deterministic_tree(
            sample_intent(), 1_440, "normal").model_dump(mode="json")

    monkeypatch.setenv("HARNESS_PROVIDER", "openai-compatible")
    monkeypatch.setattr(service, "generate_json", outputs)

    result = asyncio.run(service.generate_experiment_tree(
        sample_intent().model_dump(mode="json"), 1_440, "normal"))

    assert calls == 2
    assert sum(node.estimated_minutes for node in result.nodes) == 1_440


def test_openai_planner_rejects_non_positive_duration_before_model_call(monkeypatch):
    async def unexpected_model_call(*_args, **_kwargs):
        raise AssertionError("model must not be called")

    monkeypatch.setenv("HARNESS_PROVIDER", "openai-compatible")
    monkeypatch.setattr(service, "generate_json", unexpected_model_call)

    with pytest.raises(ValueError, match="duration_minutes must be positive"):
        asyncio.run(service.generate_experiment_tree(
            sample_intent().model_dump(mode="json"), 0, "normal"))


def test_openai_planner_accepts_a_meaningful_fifteen_minute_plan(monkeypatch):
    short_plan = deterministic_tree(sample_intent(), 15, "normal").model_dump(mode="json")

    async def short_model_output(*_args, **_kwargs):
        return short_plan

    monkeypatch.setenv("HARNESS_PROVIDER", "openai-compatible")
    monkeypatch.setattr(service, "generate_json", short_model_output)

    result = asyncio.run(service.generate_experiment_tree(
        sample_intent().model_dump(mode="json"), 15, "normal"))

    assert len(result.nodes) == 1
    assert result.nodes[0].estimated_minutes == 15


@pytest.mark.parametrize("provider", ["deterministic", "openai-compatible"])
def test_planner_rejects_subminimum_duration_before_any_model_call(monkeypatch, provider):
    async def unexpected_model_call(*_args, **_kwargs):
        raise AssertionError("model must not be called for a non-teachable duration")

    monkeypatch.setenv("HARNESS_PROVIDER", provider)
    monkeypatch.setattr(service, "generate_json", unexpected_model_call)

    with pytest.raises(ValueError, match="at least 15"):
        asyncio.run(service.generate_experiment_tree(
            sample_intent().model_dump(mode="json"), 14, "normal"))


def test_model_unavailable_uses_safe_fallback(monkeypatch):
    calls = 0

    async def unavailable_model(*_args, **_kwargs):
        nonlocal calls
        calls += 1
        raise ProviderError("model request failed with HTTP 503")

    monkeypatch.setenv("HARNESS_PROVIDER", "openai-compatible")
    monkeypatch.setattr(service, "generate_json", unavailable_model)

    result = asyncio.run(service.generate_experiment_tree(
        sample_intent().model_dump(mode="json"), 1_440, "normal"))

    assert calls == 1
    assert result.nodes[0].key == "goal"
    assert sum(node.estimated_minutes for node in result.nodes) <= 1_440


def test_model_network_timeout_uses_safe_fallback(monkeypatch):
    async def timed_out(*_args, **_kwargs):
        raise httpx.TimeoutException("upstream timed out")

    monkeypatch.setenv("HARNESS_PROVIDER", "openai-compatible")
    monkeypatch.setattr(service, "generate_json", timed_out)

    result = asyncio.run(service.generate_experiment_tree(
        sample_intent().model_dump(mode="json"), 1_440, "normal"))

    assert result.nodes[0].key == "goal"
    assert sum(node.estimated_minutes for node in result.nodes) <= 1_440


def test_repair_network_timeout_uses_safe_fallback(monkeypatch):
    calls = 0

    async def invalid_then_timeout(*_args, **_kwargs):
        nonlocal calls
        calls += 1
        if calls == 1:
            return {"nodes": []}
        raise httpx.TimeoutException("repair request timed out")

    monkeypatch.setenv("HARNESS_PROVIDER", "openai-compatible")
    monkeypatch.setattr(service, "generate_json", invalid_then_timeout)

    result = asyncio.run(service.generate_experiment_tree(
        sample_intent().model_dump(mode="json"), 1_440, "normal"))

    assert calls == 2
    assert result.nodes[0].key == "goal"


def test_unreadable_planner_skill_uses_safe_fallback(monkeypatch):
    async def unexpected_model_call(*_args, **_kwargs):
        raise AssertionError("model must not be called")

    monkeypatch.setenv("HARNESS_PROVIDER", "openai-compatible")
    monkeypatch.setattr(service, "planner_system_prompt", lambda: (_ for _ in ()).throw(
        RuntimeError("planner node-planning skill is missing or unreadable")))
    monkeypatch.setattr(service, "generate_json", unexpected_model_call)

    result = asyncio.run(service.generate_experiment_tree(
        sample_intent().model_dump(mode="json"), 1_440, "normal"))

    assert result.nodes[0].key == "goal"


def test_invalid_json_provider_response_uses_safe_tree_fallback(monkeypatch):
    calls = 0

    async def invalid_json(*_args, **_kwargs):
        nonlocal calls
        calls += 1
        raise ProviderError("model returned invalid JSON")

    monkeypatch.setenv("HARNESS_PROVIDER", "openai-compatible")
    monkeypatch.setattr(service, "generate_json", invalid_json)

    result = asyncio.run(service.generate_experiment_tree(
        sample_intent().model_dump(mode="json"), 1_440, "normal"))
    assert calls == 2
    assert len(result.nodes) == 3
