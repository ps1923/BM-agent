import asyncio
import pytest

from harnesses.requirements import service
from harnesses.requirements.service import deterministic_envelope
from harnesses.common.provider import ProviderError


def test_requirement_harness_asks_only_one_question_then_confirms():
    first = deterministic_envelope("我想做一个学习画布", None, False)
    assert first.status == "needs_clarification"
    assert first.next_question is not None
    second = deterministic_envelope("能够拖动节点、查看详情并保存数据", first, False)
    assert second.status == "ready_for_confirmation"
    assert second.next_question is None
    confirmed = deterministic_envelope("", second, True)
    assert confirmed.status == "confirmed"
    assert "nodes" not in confirmed.model_dump()


def test_requirement_harness_returns_safe_contract_after_two_invalid_model_outputs(monkeypatch):
    calls = 0

    async def invalid_model_output(*_args, **_kwargs):
        nonlocal calls
        calls += 1
        return {"not_a_student_intent": True}

    monkeypatch.setenv("HARNESS_PROVIDER", "openai-compatible")
    monkeypatch.setattr(service, "generate_json", invalid_model_output)

    result = asyncio.run(service.clarify_intent("我想做一个学习画布", None, False))

    assert calls == 2
    assert result.status == "needs_clarification"
    assert result.next_question is not None
    assert result.intent.raw_request == "我想做一个学习画布"


def test_requirement_harness_does_not_swallow_role_violation_on_fallback(monkeypatch):
    calls = 0

    async def confirmed_model_output(*_args, **_kwargs):
        nonlocal calls
        calls += 1
        return {"status": "confirmed"}

    monkeypatch.setenv("HARNESS_PROVIDER", "openai-compatible")
    monkeypatch.setattr(service, "generate_json", confirmed_model_output)

    result = asyncio.run(service.clarify_intent("我想做一个学习画布", None, False))

    assert calls == 2
    assert result.status == "needs_clarification"


def test_requirement_harness_fallback_confirms_existing_ready_intent(monkeypatch):
    calls = 0

    async def invalid_model_output(*_args, **_kwargs):
        nonlocal calls
        calls += 1
        return {"invalid": True}

    current = deterministic_envelope(
        "我要创建一个可以运行并现场演示的学习实验画布系统，并且支持节点拖动、详情展示、数据保存和结果验收。", None, False)
    monkeypatch.setenv("HARNESS_PROVIDER", "openai-compatible")
    monkeypatch.setattr(service, "generate_json", invalid_model_output)

    result = asyncio.run(service.clarify_intent(
        "", current.model_dump(mode="json"), True))

    assert calls == 2
    assert result.status == "confirmed"
    assert result.next_question is None


def test_confirmed_intent_cannot_be_reopened(monkeypatch):
    current = deterministic_envelope(
        "我要创建一个可以运行并现场演示的学习实验画布系统，并且支持节点拖动、详情展示、数据保存和结果验收。", None, False)
    current = deterministic_envelope("", current, True)
    monkeypatch.setenv("HARNESS_PROVIDER", "openai-compatible")

    with pytest.raises(ValueError, match="confirmed intent is immutable"):
        asyncio.run(service.clarify_intent("改成别的方向", current.model_dump(mode="json"), False))


def test_invalid_json_provider_response_uses_safe_fallback(monkeypatch):
    calls = 0

    async def invalid_json(*_args, **_kwargs):
        nonlocal calls
        calls += 1
        raise ProviderError("model returned invalid JSON")

    monkeypatch.setenv("HARNESS_PROVIDER", "openai-compatible")
    monkeypatch.setattr(service, "generate_json", invalid_json)

    result = asyncio.run(service.clarify_intent("我想做一个学习画布", None, False))
    assert calls == 2
    assert result.status == "needs_clarification"
