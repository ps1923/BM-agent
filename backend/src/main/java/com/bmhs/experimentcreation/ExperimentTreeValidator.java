package com.bmhs.experimentcreation;

import com.bmhs.experimentcreation.CreationModels.ExperimentTree;
import com.bmhs.experimentcreation.CreationModels.PlanNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ExperimentTreeValidator {
    private static final Set<String> VAGUE_TASKS = Set.of("学习", "了解", "熟悉", "掌握", "研究");
    private static final Set<String> VALIDATION_TYPES = Set.of("manual", "command", "test", "agent");

    public void validate(ExperimentTree tree, int durationMinutes) {
        List<String> errors = new ArrayList<>();
        if (tree == null || tree.nodes() == null) {
            fail(List.of("计划中没有节点"));
        }
        if (!"0.1".equals(tree.schemaVersion())) errors.add("计划协议版本不受支持");
        if (blankOrLong(tree.title(), 150)) errors.add("实验名称长度不合法");
        if (tree.purpose() == null || tree.purpose().isEmpty() || tree.purpose().size() > 8
                || tree.purpose().stream().anyMatch(item -> item == null || item.isBlank())) {
            errors.add("实验目的不能为空且最多 8 项");
        }
        if (tree.nodes().isEmpty()) errors.add("实验流程至少需要 1 个节点");
        if (tree.nodes().size() > 80) errors.add("实验流程最多允许 80 个节点");

        Map<String, PlanNode> byKey = new HashMap<>();
        int roots = 0;
        long totalMinutes = 0;
        for (PlanNode node : tree.nodes()) {
            if (node == null) {
                errors.add("节点不能为空");
                continue;
            }
            if (node.key() == null || node.key().isBlank()) {
                errors.add("节点 key 不能为空");
                continue;
            }
            if (!node.key().matches("^[a-z0-9][a-z0-9-]{0,29}$")) errors.add("节点 key 格式不合法: " + node.key());
            if (byKey.put(node.key(), node) != null) errors.add("节点 key 重复: " + node.key());
            if (node.parentKey() == null) roots++;
            else if (node.parentKey().isBlank()) errors.add("parentKey 为空白时必须使用 null: " + node.key());
            if (node.depth() < 0 || node.depth() > 8) errors.add("节点深度必须在 0–8 之间: " + node.key());
            if (blankOrLong(node.stage(), 150)) errors.add("节点阶段长度不合法: " + node.key());
            if (blankOrLong(node.title(), 150)) errors.add("节点名称长度不合法: " + node.key());
            if (node.description() == null || node.description().isBlank()) errors.add("节点说明不能为空: " + node.key());
            if (node.estimatedMinutes() <= 0 || node.estimatedMinutes() > 1440) {
                errors.add("单节点预计工时必须在 1–1440 分钟之间: " + node.key());
            }
            if (!"all_prerequisites_completed".equals(node.unlockRule())) {
                errors.add("节点解锁规则不受支持: " + node.key());
            }
            if (!"all_tasks_completed".equals(node.completionRule())) {
                errors.add("节点完成规则不受支持: " + node.key());
            }
            if (node.prerequisiteKeys() == null) {
                errors.add("节点前置列表不能为空: " + node.key());
            }
            totalMinutes += node.estimatedMinutes();
            if (node.tasks() == null || node.tasks().size() < 3 || node.tasks().size() > 7) {
                errors.add("每个节点必须包含 3–7 个任务: " + node.key());
            } else {
                node.tasks().forEach(task -> {
                    if (task == null) {
                        errors.add("任务不能为空: " + node.key());
                        return;
                    }
                    String title = task.title() == null ? "" : task.title().trim();
                    boolean vague = VAGUE_TASKS.stream().anyMatch(word ->
                            title.equals(word) || title.startsWith(word + " ") || title.startsWith(word));
                    if (title.length() < 2 || title.length() > 200 || vague) {
                        errors.add("任务必须是具体动作: " + node.key());
                    }
                    if (task.description() == null || task.description().length() < 2) {
                        errors.add("任务说明不能为空: " + node.key());
                    }
                    if (task.validationType() == null || !VALIDATION_TYPES.contains(task.validationType())) {
                        errors.add("任务验收类型不受支持: " + node.key());
                    }
                });
            }
        }
        if (roots != 1) errors.add("实验树必须且只能有一个根节点");

        for (PlanNode node : tree.nodes()) {
            if (node.parentKey() == null) {
                if (node.depth() != 0) errors.add("根节点深度必须为 0");
                if (node.prerequisiteKeys() != null && !node.prerequisiteKeys().isEmpty()) {
                    errors.add("根节点不能有前置节点");
                }
                continue;
            }
            PlanNode parent = byKey.get(node.parentKey());
            if (parent == null) {
                errors.add("父节点不存在: " + node.parentKey());
            } else if (node.depth() != parent.depth() + 1) {
                errors.add("节点深度与父节点不一致: " + node.key());
            }
            if (node.prerequisiteKeys() == null
                    || node.prerequisiteKeys().size() != 1
                    || !node.parentKey().equals(node.prerequisiteKeys().get(0))) {
                errors.add("节点前置必须且只能指向 parentKey: " + node.key());
            }
        }

        detectCycles(byKey, errors);
        int allowedMinutes = Math.max(durationMinutes + 60, (int) Math.ceil(durationMinutes * 1.2));
        if (totalMinutes > allowedMinutes) errors.add("节点预计总工时明显超过学习周期");
        if (totalMinutes <= 0) errors.add("节点预计工时必须大于零");
        if (!errors.isEmpty()) fail(errors);
    }

    private void detectCycles(Map<String, PlanNode> byKey, List<String> errors) {
        for (PlanNode node : byKey.values()) {
            Set<String> path = new HashSet<>();
            PlanNode cursor = node;
            while (cursor != null && cursor.parentKey() != null && !cursor.parentKey().isBlank()) {
                if (!path.add(cursor.key())) {
                    errors.add("节点父子关系存在环路");
                    return;
                }
                cursor = byKey.get(cursor.parentKey());
            }
        }
    }

    private void fail(List<String> errors) {
        throw new ApiException(HttpStatus.BAD_GATEWAY, "INVALID_EXPERIMENT_TREE", String.join("；", errors));
    }

    private boolean blankOrLong(String value, int maximum) {
        return value == null || value.isBlank() || value.length() > maximum;
    }
}
