import asyncio
import pytest

from harnesses.common.contracts import ConfirmedIntent, CurrentState, ExperimentTree, IntentConstraints, IntentPreferences, StudentIntent
from harnesses.planner import service
from harnesses.planner.service import deterministic_tree
from harnesses.common.provider import ProviderError


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
        assert len(tree.nodes[0].tasks) >= 5
        assert sum(node.estimated_minutes for node in tree.nodes) <= minutes


def test_general_environment_note_does_not_claim_required_stack_is_ready():
    intent = sample_intent()
    intent.intent.constraints.required_stack = ["Python"]
    intent.intent.constraints.environment = ["Linux 学校机房"]

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

    assert all(1 <= node.estimated_minutes <= 1440 for node in tree.nodes)
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
        service._validate_model_tree(plan, 1_440)


def test_planner_prompt_loads_recursive_learner_centered_skill():
    prompt = service.planner_system_prompt()

    assert "继续递归拆成子节点" in prompt
    assert "3.1.1" in prompt
    assert "父节点自身也必须有真实的 3–7 项任务" in prompt
    assert "当前合同只支持单父节点和单一父依赖" in prompt


def test_model_plan_accepts_multilevel_learning_nodes(monkeypatch):
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
            plan_node("required-fields", "request-validation", 2, "3.1.1 校验必填字段"),
            plan_node("field-format", "request-validation", 2, "3.1.2 校验字段格式"),
        ],
    }

    async def nested_model_output(system, *_args, **_kwargs):
        nonlocal model_prompt
        model_prompt = system
        return nested_tree

    monkeypatch.setenv("HARNESS_PROVIDER", "openai-compatible")
    monkeypatch.setattr(service, "generate_json", nested_model_output)

    result = asyncio.run(service.generate_experiment_tree(
        sample_intent().model_dump(mode="json"), 1_440, "normal"))

    assert model_prompt is not None and "3.1.1" in model_prompt
    assert [(node.depth, node.title) for node in result.nodes] == [
        (0, "3 实现核心接口"),
        (1, "3.1 实现请求校验"),
        (2, "3.1.1 校验必填字段"),
        (2, "3.1.2 校验字段格式"),
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


def test_openai_planner_rejects_unusable_duration_before_model_call(monkeypatch):
    async def unexpected_model_call(*_args, **_kwargs):
        raise AssertionError("model must not be called")

    monkeypatch.setenv("HARNESS_PROVIDER", "openai-compatible")
    monkeypatch.setattr(service, "generate_json", unexpected_model_call)

    with pytest.raises(ValueError, match="at least 15"):
        asyncio.run(service.generate_experiment_tree(
            sample_intent().model_dump(mode="json"), 14, "normal"))


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
