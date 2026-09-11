from __future__ import annotations

import json
import os
from typing import Any

import httpx


class ProviderError(RuntimeError):
    pass


def parse_json_object(content: Any) -> dict[str, Any]:
    if not isinstance(content, str) or not content.strip():
        raise ProviderError("model returned invalid JSON")
    text = content.strip()
    try:
        parsed = json.loads(text)
    except json.JSONDecodeError:
        start = text.find("{")
        if start < 0:
            raise ProviderError("model returned invalid JSON")
        try:
            parsed, _ = json.JSONDecoder().raw_decode(text[start:])
        except json.JSONDecodeError as exc:
            raise ProviderError("model returned invalid JSON") from exc
    if not isinstance(parsed, dict):
        raise ProviderError("model returned invalid JSON")
    return parsed


async def generate_json(
    system_prompt: str,
    payload: dict[str, Any],
    model_env: str,
    json_schema: dict[str, Any],
) -> dict[str, Any]:
    provider = os.getenv("HARNESS_PROVIDER", "deterministic")
    if provider == "deterministic":
        raise ProviderError("deterministic provider must be handled by the Harness")
    if provider != "openai-compatible":
        raise ProviderError(f"unsupported HARNESS_PROVIDER: {provider}")

    api_key = os.getenv("AI_API_KEY")
    model = os.getenv(model_env)
    base_url = os.getenv("AI_BASE_URL", "https://api.openai.com/v1").rstrip("/")
    if not api_key or not model:
        raise ProviderError(f"AI_API_KEY and {model_env} are required")

    schema_text = json.dumps(json_schema, ensure_ascii=False, separators=(",", ":"))
    constrained_prompt = (
        f"{system_prompt}\n\n"
        "你必须只返回一个 JSON 对象，不得返回解释、Markdown 或替代字段。"
        "所有必填字段、枚举、嵌套对象和 extraProperties 限制必须严格符合下面的 JSON Schema：\n"
        f"{schema_text}"
    )
    body = {
        "model": model,
        "temperature": 0.2,
        "response_format": {"type": "json_object"},
        "messages": [
            {"role": "system", "content": constrained_prompt},
            {"role": "user", "content": json.dumps(payload, ensure_ascii=False)},
        ],
    }
    async with httpx.AsyncClient(timeout=120) as client:
        response = await client.post(
            f"{base_url}/chat/completions",
            headers={"Authorization": f"Bearer {api_key}"},
            json=body,
        )
    if response.is_error:
        raise ProviderError(f"model request failed with HTTP {response.status_code}")
    try:
        content = response.json()["choices"][0]["message"]["content"]
        return parse_json_object(content)
    except (KeyError, IndexError, TypeError) as exc:
        raise ProviderError("model returned invalid JSON") from exc
