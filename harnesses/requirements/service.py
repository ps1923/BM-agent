from __future__ import annotations

import logging
import os
from typing import Any
from pydantic import ValidationError

from mcp.server import MCPServer
from starlette.requests import Request
from starlette.responses import JSONResponse

from harnesses.common.contracts import (
    CurrentState,
    IntentConstraints,
    IntentPreferences,
    NextQuestion,
    QuestionOption,
    StudentIntent,
    StudentIntentEnvelope,
)
from harnesses.common.provider import ProviderError, generate_json


SYSTEM_PROMPT = """
你是需求确认 Harness，只能澄清学生想做或学习的实验方向。
每轮最多提出一个会改变交付物、技术范围、起点或验收方式的问题。
绝对禁止生成实验阶段、节点、任务、代码、SQL 或工作区。
不得询问周期和难度，它们由后续界面收集。
区分学生明确事实、可撤销假设和缺失信息；不要把推断伪装为事实。
信息充分时返回 ready_for_confirmation，只有 confirm=true 时才返回 confirmed。
严格返回符合 StudentIntentEnvelope 的 JSON，不要输出额外文字。
""".strip()

mcp = MCPServer("requirements-harness")
logger = logging.getLogger(__name__)


def deterministic_envelope(
    message: str,
    current: StudentIntentEnvelope | None,
    confirm: bool,
) -> StudentIntentEnvelope:
    if confirm:
        if current is None or current.status not in {"ready_for_confirmation", "confirmed"}:
            raise ValueError("only a ready intent can be confirmed")
        return current.model_copy(update={
            "status": "confirmed",
            "assistant_message": "实验方向已经确认，接下来请选择学习周期和难度。",
            "missing_critical_fields": [],
            "next_question": None,
        })

    clean = message.strip()
    if not clean:
        raise ValueError("message cannot be empty")

    if current is None:
        intent = StudentIntent(
            raw_request=clean,
            observable_outcome=clean,
            target_artifact="可运行、可现场演示的实验成果",
            current_state=CurrentState(),
            constraints=IntentConstraints(),
            preferences=IntentPreferences(),
            success_criteria=["能够运行目标功能并现场演示主要交互", "关键结果可以通过操作或测试验证"],
        )
        if len(clean) < 45:
            return StudentIntentEnvelope(
                status="needs_clarification",
                assistant_message="我已经记下方向。还需要确认最终要做到什么程度。",
                intent=intent,
                field_evidence={
                    "observable_outcome": "explicit",
                    "target_artifact": "inferred",
                    "current_state": "missing",
                    "time_budget": "missing",
                    "success_criteria": "inferred",
                },
                missing_critical_fields=["target_artifact"],
                assumptions=["暂定交付物是一个可运行、可现场演示的功能"],
                next_question=NextQuestion(
                    id="target-demonstration",
                    text="实验完成后，你希望现场能够演示哪些具体功能或结果？",
                    answer_type="free_text",
                    options=[],
                    why_it_matters="这会决定实验边界和最终验收方式。",
                ),
            )
        status = "ready_for_confirmation"
        assistant = f"我的理解是：围绕“{clean}”完成一个可以运行并现场验证的实验成果。请确认这个方向。"
        return StudentIntentEnvelope(
            status=status,
            assistant_message=assistant,
            intent=intent,
            field_evidence={
                "observable_outcome": "explicit", "target_artifact": "inferred",
                "current_state": "missing", "time_budget": "missing", "success_criteria": "inferred",
            },
            assumptions=["暂按可运行、可现场演示的成果设计"],
        )

    merged_intent = current.intent.model_copy(update={
        "raw_request": f"{current.intent.raw_request}\n补充：{clean}",
        "target_artifact": clean,
        "success_criteria": [clean, "能够运行目标功能并现场验证关键结果"],
    })
    return StudentIntentEnvelope(
        status="ready_for_confirmation",
        assistant_message=(
            f"我的理解是：你要完成“{current.intent.observable_outcome}”，最终成果是“{clean}”，"
            "并以能够现场运行和验证为完成标准。请确认这个方向。"
        ),
        intent=merged_intent,
        field_evidence={
            "observable_outcome": "explicit", "target_artifact": "explicit",
            "current_state": "missing", "time_budget": "missing", "success_criteria": "explicit",
        },
        assumptions=[],
    )


def _validate_model_envelope(result: Any, confirm: bool) -> StudentIntentEnvelope:
    envelope = StudentIntentEnvelope.model_validate(result)
    if confirm and envelope.status != "confirmed":
        raise ValueError("confirmation call must return confirmed")
    if not confirm and envelope.status == "confirmed":
        raise ValueError("Harness cannot confirm without explicit user confirmation")
    return envelope


def _safe_fallback(message: str, current: StudentIntentEnvelope | None,
                   confirm: bool) -> StudentIntentEnvelope:
    """Return a valid requirements-only result after two model contract failures."""
    if confirm:
        if current is None or current.status not in {"ready_for_confirmation", "confirmed"}:
            raise ValueError("only a ready intent can be confirmed")
        return deterministic_envelope("", current, True)
    if current is not None and (current.status == "needs_clarification" or not message.strip()):
        return current
    return deterministic_envelope(message.strip() or "请说明希望完成的实验方向。", current, False)


@mcp.tool()
async def clarify_intent(
    message: str,
    current_intent: dict[str, Any] | None = None,
    confirm: bool = False,
) -> StudentIntentEnvelope:
    """Only clarify or confirm a student's experiment direction; never create nodes or code."""
    current = StudentIntentEnvelope.model_validate(current_intent) if current_intent else None
    if current is not None and current.status == "confirmed" and not confirm:
        raise ValueError("confirmed intent is immutable")
    if os.getenv("HARNESS_PROVIDER", "deterministic") == "deterministic":
        return deterministic_envelope(message, current, confirm)
    request_payload = {"message": message, "current_intent": current_intent, "confirm": confirm}
    schema = StudentIntentEnvelope.model_json_schema()
    try:
        result = await generate_json(SYSTEM_PROMPT, request_payload, "REQUIREMENTS_MODEL", schema)
    except ProviderError as error:
        if str(error) != "model returned invalid JSON":
            raise
        result = {"_model_output_invalid": True}
    try:
        envelope = _validate_model_envelope(result, confirm)
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
            "instruction": "只修复格式和合同错误，不改变用户已表达的事实。",
        }
        try:
            repaired = await generate_json(
                SYSTEM_PROMPT + "\n当前请求是格式修复，必须返回完整合法对象。",
                repair_payload,
                "REQUIREMENTS_MODEL",
                schema,
            )
        except ProviderError as second_error:
            logger.warning(
                "requirements model repair failed; using safe fallback (failure=%s)",
                type(second_error).__name__,
            )
            return _safe_fallback(message, current, confirm)
        try:
            envelope = _validate_model_envelope(repaired, confirm)
        except (ValidationError, ValueError) as second_error:
            logger.warning(
                "requirements model output failed contract validation twice; using safe fallback "
                "(failure=%s)",
                type(second_error).__name__,
            )
            envelope = _safe_fallback(message, current, confirm)
    return envelope


@mcp.custom_route("/health", methods=["GET"])
async def health(_: Request) -> JSONResponse:
    if os.getenv("HARNESS_PROVIDER", "deterministic") == "openai-compatible" and (
        not os.getenv("AI_API_KEY") or not os.getenv("REQUIREMENTS_MODEL")
    ):
        return JSONResponse({"status": "not_ready", "harness": "requirements"}, status_code=503)
    return JSONResponse({"status": "ok", "harness": "requirements"})


if __name__ == "__main__":
    mcp.run(
        transport="streamable-http",
        host=os.getenv("HARNESS_HOST", "127.0.0.1"),
        port=int(os.getenv("PORT", "8101")),
        stateless_http=True,
        json_response=True,
    )
