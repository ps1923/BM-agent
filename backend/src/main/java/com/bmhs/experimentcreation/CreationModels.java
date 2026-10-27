package com.bmhs.experimentcreation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class CreationModels {
    private CreationModels() {}

    public record CreateSessionResponse(String sessionId, String status) {}

    public record DirectionMessageRequest(@NotBlank @Size(max = 4000) String message) {}

    public record NextQuestion(
            String id,
            String text,
            String answerType,
            List<Map<String, String>> options,
            String whyItMatters) {}

    public record StudentIntentEnvelope(
            String schemaVersion,
            String status,
            String assistantMessage,
            Map<String, Object> intent,
            Map<String, String> fieldEvidence,
            List<String> missingCriticalFields,
            List<String> assumptions,
            NextQuestion nextQuestion) {}

    public record ConfirmedIntent(
            String schemaVersion,
            String intentHash,
            String confirmedAt,
            Map<String, Object> intent) {}

    public record ConfigurationRequest(
            @Min(0) @Max(3650) int durationDays,
            @Min(0) @Max(23) int durationHours,
            @NotNull Difficulty difficulty) {
        public int durationMinutes() {
            return Math.addExact(Math.multiplyExact(durationDays, 1_440), Math.multiplyExact(durationHours, 60));
        }
    }

    public enum Difficulty {
        BEGINNER("入门"), EASY("简单"), NORMAL("普通"), HARD("困难"), CHALLENGE("挑战");

        private final String label;

        Difficulty(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public record PlanTask(
            String title,
            String description,
            String validationType,
            Map<String, Object> validationConfig) {}

    public record AcceptanceCriterion(
            String criterion,
            String verificationMethod,
            String expectedEvidence) {}

    public record PositionedTask(
            Long taskId,
            String title,
            String description,
            String validationType,
            Map<String, Object> validationConfig) {}

    public record PlanNode(
            String key,
            String parentKey,
            List<String> prerequisiteKeys,
            String unlockRule,
            String completionRule,
            int depth,
            String stage,
            String title,
            String description,
            int estimatedMinutes,
            List<PlanTask> tasks,
            List<AcceptanceCriterion> acceptanceCriteria) {
        public PlanNode {
            prerequisiteKeys = prerequisiteKeys == null
                    ? (parentKey == null ? List.of() : List.of(parentKey))
                    : List.copyOf(prerequisiteKeys);
            unlockRule = unlockRule == null ? "all_prerequisites_completed" : unlockRule;
            completionRule = completionRule == null ? "all_tasks_completed" : completionRule;
            acceptanceCriteria = acceptanceCriteria == null || acceptanceCriteria.isEmpty()
                    ? inferredCriteria(tasks) : List.copyOf(acceptanceCriteria);
        }

        private static List<AcceptanceCriterion> inferredCriteria(List<PlanTask> tasks) {
            if (tasks == null) return List.of();
            return tasks.stream().filter(java.util.Objects::nonNull).map(task -> new AcceptanceCriterion(
                    task.title() == null || task.title().isBlank() ? "检查节点成果" : task.title(),
                    "manual".equals(task.validationType()) ? "manual_demo"
                            : "static_code".equals(task.validationType()) || "agent".equals(task.validationType())
                            ? "static_code" : "test_artifact",
                    task.description() == null || task.description().isBlank() ? "核对节点成果" : task.description()
            )).toList();
        }

        public PlanNode(String key, String parentKey, int depth, String stage, String title,
                        String description, int estimatedMinutes, List<PlanTask> tasks) {
            this(key, parentKey, parentKey == null ? List.of() : List.of(parentKey),
                    "all_prerequisites_completed", "all_tasks_completed", depth, stage, title,
                    description, estimatedMinutes, tasks, List.of());
        }

        public PlanNode(String key, String parentKey, List<String> prerequisiteKeys,
                        String unlockRule, String completionRule, int depth, String stage, String title,
                        String description, int estimatedMinutes, List<PlanTask> tasks) {
            this(key, parentKey, prerequisiteKeys, unlockRule, completionRule, depth, stage, title,
                    description, estimatedMinutes, tasks, List.of());
        }
    }

    public record ExperimentTree(
            String schemaVersion,
            String title,
            List<String> purpose,
            List<PlanNode> nodes) {}

    public record PositionedNode(
            String key,
            String parentKey,
            List<String> prerequisiteKeys,
            String unlockRule,
            String completionRule,
            int depth,
            String stage,
            String title,
            String description,
            int estimatedMinutes,
            List<PositionedTask> tasks,
            BigDecimal canvasX,
            BigDecimal canvasY,
            Long nodeId,
            List<AcceptanceCriterion> acceptanceCriteria,
            Integer acceptanceCriteriaVersion,
            String acceptanceCriteriaSource) {
        public PositionedNode {
            prerequisiteKeys = prerequisiteKeys == null
                    ? (parentKey == null ? List.of() : List.of(parentKey))
                    : List.copyOf(prerequisiteKeys);
            unlockRule = unlockRule == null ? "all_prerequisites_completed" : unlockRule;
            completionRule = completionRule == null ? "all_tasks_completed" : completionRule;
            acceptanceCriteria = acceptanceCriteria == null || acceptanceCriteria.isEmpty()
                    ? inferPositionedCriteria(tasks) : List.copyOf(acceptanceCriteria);
            if ((acceptanceCriteriaVersion == null || acceptanceCriteriaVersion <= 0)
                    && (acceptanceCriteriaSource == null || acceptanceCriteriaSource.isBlank())) {
                acceptanceCriteriaSource = "legacy_inferred";
            }
        }

        private static List<AcceptanceCriterion> inferPositionedCriteria(List<PositionedTask> tasks) {
            if (tasks == null) return List.of();
            return tasks.stream().filter(java.util.Objects::nonNull).map(task -> new AcceptanceCriterion(
                    task.title() == null || task.title().isBlank() ? "检查节点成果" : task.title(),
                    "manual".equals(task.validationType()) ? "manual_demo"
                            : "agent".equals(task.validationType()) ? "static_code" : "test_artifact",
                    task.description() == null || task.description().isBlank() ? "核对节点成果" : task.description()
            )).toList();
        }

        public PositionedNode(String key, String parentKey, int depth, String stage, String title,
                              String description, int estimatedMinutes, List<PositionedTask> tasks,
                              BigDecimal canvasX, BigDecimal canvasY) {
            this(key, parentKey, parentKey == null ? List.of() : List.of(parentKey),
                    "all_prerequisites_completed", "all_tasks_completed", depth, stage, title,
                    description, estimatedMinutes, tasks, canvasX, canvasY, null, List.of(), null, "legacy_inferred");
        }

        public PositionedNode(String key, String parentKey, int depth, String stage, String title,
                              String description, int estimatedMinutes, List<PositionedTask> tasks,
                              BigDecimal canvasX, BigDecimal canvasY, Long nodeId) {
            this(key, parentKey, parentKey == null ? List.of() : List.of(parentKey),
                    "all_prerequisites_completed", "all_tasks_completed", depth, stage, title,
                    description, estimatedMinutes, tasks, canvasX, canvasY, nodeId, List.of(), null, "legacy_inferred");
        }

        public PositionedNode(String key, String parentKey, List<String> prerequisiteKeys,
                              String unlockRule, String completionRule, int depth, String stage, String title,
                              String description, int estimatedMinutes, List<PositionedTask> tasks,
                              BigDecimal canvasX, BigDecimal canvasY, Long nodeId) {
            this(key, parentKey, prerequisiteKeys, unlockRule, completionRule, depth, stage, title,
                    description, estimatedMinutes, tasks, canvasX, canvasY, nodeId, List.of(), null, "legacy_inferred");
        }

        public PositionedNode(String key, String parentKey, int depth, String stage, String title,
                              String description, int estimatedMinutes, List<PositionedTask> tasks,
                              BigDecimal canvasX, BigDecimal canvasY, Long nodeId,
                              List<AcceptanceCriterion> acceptanceCriteria, Integer acceptanceCriteriaVersion,
                              String acceptanceCriteriaSource) {
            this(key, parentKey, parentKey == null ? List.of() : List.of(parentKey),
                    "all_prerequisites_completed", "all_tasks_completed", depth, stage, title, description,
                    estimatedMinutes, tasks, canvasX, canvasY, nodeId, acceptanceCriteria,
                    acceptanceCriteriaVersion, acceptanceCriteriaSource);
        }
    }

    public record PlanResponse(
            String planId,
            int version,
            String intentHash,
            String title,
            List<String> purpose,
            List<PositionedNode> nodes) {}

    public record MaterializedExperiment(
            long experimentId,
            String name,
            String description,
            String difficulty,
            int durationMinutes,
            List<PositionedNode> nodes) {}

    public record SessionView(
            String sessionId,
            String status,
            StudentIntentEnvelope direction,
            Integer durationMinutes,
            Difficulty difficulty,
            PlanResponse plan,
            Long experimentId) {}

    public record SessionDraft(
            String sessionId,
            String status,
            String updatedAt,
            String title,
            Integer durationMinutes,
            Difficulty difficulty) {}

    public record ConversationMessage(
            String role,
            String content,
            String createdAt) {}
}
