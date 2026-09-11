"""Live Streamable HTTP smoke test for both isolated Harness servers."""

import asyncio

from mcp import Client


async def main() -> None:
    async with Client("http://127.0.0.1:8101/mcp") as requirements:
        tools = await requirements.list_tools()
        assert [tool.name for tool in tools.tools] == ["clarify_intent"]
        first = await requirements.call_tool(
            "clarify_intent",
            {"message": "我想做一个学习画布", "current_intent": None, "confirm": False},
        )
        first_payload = first.structured_content
        assert first_payload is not None
        assert first_payload["status"] in {"needs_clarification", "ready_for_confirmation"}

    confirmed_intent = {
        "schema_version": "0.1",
        "intent_hash": "a" * 64,
        "confirmed_at": "2026-09-11T00:00:00Z",
        "intent": {
            "raw_request": "创建学习画布",
            "observable_outcome": "创建能够拖动节点并查看详情的学习画布",
            "target_artifact": "可运行的 Web 应用",
            "current_state": {"known_skills": [], "relevant_experience": "", "starting_assets": []},
            "constraints": {
                "time_budget_minutes": None, "deadline": None, "required_stack": [],
                "environment": [], "excluded_scope": [],
            },
            "preferences": {"difficulty": None, "guidance_level": None, "language": "zh-CN"},
            "success_criteria": ["可以拖动节点", "可以查看详情"],
        },
    }
    async with Client("http://127.0.0.1:8102/mcp") as planner:
        tools = await planner.list_tools()
        assert [tool.name for tool in tools.tools] == ["generate_experiment_tree"]
        result = await planner.call_tool(
            "generate_experiment_tree",
            {"confirmed_intent": confirmed_intent, "duration_minutes": 1440, "difficulty": "normal"},
        )
        payload = result.structured_content
        assert payload is not None
        assert len(payload["nodes"]) >= 3
        assert all(
            node["prerequisite_keys"] == ([] if node["parent_key"] is None else [node["parent_key"]])
            for node in payload["nodes"]
        )
        assert all(node["unlock_rule"] == "all_prerequisites_completed" for node in payload["nodes"])
        assert all(node["completion_rule"] == "all_tasks_completed" for node in payload["nodes"])
        assert not any(key in payload for key in ("messages", "conversation"))
    print(f"requirements=1 tool, planner=1 tool, tree={len(payload['nodes'])} nodes: OK")


if __name__ == "__main__":
    asyncio.run(main())
