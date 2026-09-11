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
    assert widths == {depth: 1 for depth in range(4)}


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


def test_planner_scales_fallback_to_confirmed_workload():
    intent = sample_intent()
    intent.intent.raw_request = "用 HTML、Python 和 MySQL 完成可运行的数据页面"
    intent.intent.constraints.required_stack.extend(["HTML", "Python", "MySQL"])
    intent.intent.success_criteria.append("完成验收并记录结果")
    tree = deterministic_tree(intent, 1440, "normal")

    assert len(tree.nodes) == 7
    assert tree.nodes[-1].key == "acceptance"


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
    assert len(result.nodes) == 4
    assert {node.depth for node in result.nodes} == set(range(4))


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
    assert sum(node.estimated_minutes for node in result.nodes) <= 1_500


def test_openai_planner_rejects_non_positive_duration_before_model_call(monkeypatch):
    async def unexpected_model_call(*_args, **_kwargs):
        raise AssertionError("model must not be called")

    monkeypatch.setenv("HARNESS_PROVIDER", "openai-compatible")
    monkeypatch.setattr(service, "generate_json", unexpected_model_call)

    with pytest.raises(ValueError, match="duration_minutes must be positive"):
        asyncio.run(service.generate_experiment_tree(
            sample_intent().model_dump(mode="json"), 0, "normal"))


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
    assert len(result.nodes) == 4
