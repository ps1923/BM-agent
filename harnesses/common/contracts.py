from __future__ import annotations

from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field, model_validator


class ContractModel(BaseModel):
    model_config = ConfigDict(extra="forbid")


class QuestionOption(ContractModel):
    value: str
    label: str


class NextQuestion(ContractModel):
    id: str
    text: str
    answer_type: Literal["free_text", "single_select", "multi_select"]
    options: list[QuestionOption] = Field(default_factory=list)
    why_it_matters: str


class CurrentState(ContractModel):
    known_skills: list[str] = Field(default_factory=list)
    relevant_experience: str = ""
    starting_assets: list[str] = Field(default_factory=list)


class IntentConstraints(ContractModel):
    time_budget_minutes: int | None = None
    deadline: str | None = None
    required_stack: list[str] = Field(default_factory=list)
    environment: list[str] = Field(default_factory=list)
    excluded_scope: list[str] = Field(default_factory=list)


class IntentPreferences(ContractModel):
    difficulty: str | None = None
    guidance_level: str | None = None
    language: str = "zh-CN"


class StudentIntent(ContractModel):
    raw_request: str
    observable_outcome: str
    target_artifact: str
    current_state: CurrentState
    constraints: IntentConstraints
    preferences: IntentPreferences
    success_criteria: list[str] = Field(min_length=1, max_length=12)


class StudentIntentEnvelope(ContractModel):
    schema_version: Literal["0.1"] = "0.1"
    status: Literal["needs_clarification", "ready_for_confirmation", "confirmed"]
    assistant_message: str
    intent: StudentIntent
    field_evidence: dict[str, Literal["explicit", "inferred", "missing"]]
    missing_critical_fields: list[str] = Field(default_factory=list)
    assumptions: list[str] = Field(default_factory=list)
    next_question: NextQuestion | None = None

    @model_validator(mode="after")
    def validate_status_shape(self) -> "StudentIntentEnvelope":
        if self.status == "needs_clarification":
            if self.next_question is None or not self.missing_critical_fields:
                raise ValueError("clarification state requires one question and a missing field")
        elif self.next_question is not None or self.missing_critical_fields:
            raise ValueError("ready and confirmed states cannot contain a question or missing critical fields")
        return self


class ConfirmedIntent(ContractModel):
    schema_version: Literal["0.1"]
    intent_hash: str = Field(min_length=64, max_length=64)
    confirmed_at: str
    intent: StudentIntent


class PlanTask(ContractModel):
    title: str = Field(min_length=2, max_length=200)
    description: str = Field(min_length=2)
    validation_type: Literal["manual", "command", "test", "agent"] = "manual"
    validation_config: dict[str, Any] = Field(default_factory=dict)


class PlanNode(ContractModel):
    key: str = Field(pattern=r"^[a-z0-9][a-z0-9-]{0,29}$")
    parent_key: str | None = None
    prerequisite_keys: list[str] = Field(default_factory=list)
    unlock_rule: Literal["all_prerequisites_completed"] = "all_prerequisites_completed"
    completion_rule: Literal["all_tasks_completed"] = "all_tasks_completed"
    depth: int = Field(ge=0, le=8)
    stage: str = Field(min_length=2, max_length=150)
    title: str = Field(min_length=2, max_length=150)
    description: str = Field(min_length=2)
    estimated_minutes: int = Field(gt=0, le=1440)
    tasks: list[PlanTask] = Field(min_length=3, max_length=7)


class ExperimentTree(ContractModel):
    schema_version: Literal["0.1"] = "0.1"
    title: str = Field(min_length=2, max_length=150)
    purpose: list[str] = Field(min_length=1, max_length=8)
    nodes: list[PlanNode] = Field(min_length=1, max_length=80)

    @model_validator(mode="after")
    def validate_rooted_dependency_flow(self) -> "ExperimentTree":
        by_key = {node.key: node for node in self.nodes}
        if len(by_key) != len(self.nodes):
            raise ValueError("node keys must be unique")
        roots = [node for node in self.nodes if node.parent_key is None]
        if len(roots) != 1 or roots[0].depth != 0:
            raise ValueError("tree must have exactly one depth-0 root")
        for node in self.nodes:
            if node.parent_key is not None:
                parent = by_key.get(node.parent_key)
                if parent is None or node.depth != parent.depth + 1:
                    raise ValueError(f"invalid parent for {node.key}")
                if node.prerequisite_keys != [node.parent_key]:
                    raise ValueError(f"prerequisites must match parent for {node.key}")
            elif node.prerequisite_keys:
                raise ValueError("root node cannot have prerequisites")
            seen: set[str] = set()
            cursor = node
            while cursor.parent_key is not None:
                if cursor.key in seen:
                    raise ValueError("tree contains a cycle")
                seen.add(cursor.key)
                parent = by_key.get(cursor.parent_key)
                if parent is None:
                    raise ValueError(f"invalid parent for {cursor.key}")
                cursor = parent
        return self
