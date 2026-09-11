package com.bmhs.experimentcreation;

import com.bmhs.experimentcreation.CreationModels.ExperimentTree;
import com.bmhs.experimentcreation.CreationModels.PlanNode;
import com.bmhs.experimentcreation.CreationModels.PlanTask;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExperimentTreeValidatorTest {
    private final ExperimentTreeValidator validator = new ExperimentTreeValidator();

    @Test
    void acceptsRootedDependencyTree() {
        validator.validate(tree(false), 600);
    }

    @Test
    void acceptsNaturalLinearPlan() {
        List<PlanNode> nodes = new ArrayList<>();
        String parent = null;
        for (int depth = 0; depth < 3; depth++) {
            String key = "node-" + depth;
            nodes.add(node(key, parent, depth));
            parent = key;
        }
        ExperimentTree linear = new ExperimentTree("0.1", "线性计划", List.of("测试"), nodes);
        assertThatCode(() -> validator.validate(linear, 600)).doesNotThrowAnyException();
    }

    @Test
    void acceptsSingleStagePlanWhenWorkloadIsSmall() {
        ExperimentTree single = new ExperimentTree("0.1", "小实验", List.of("完成一个结果"),
                List.of(node("root", null, 0)));
        assertThatCode(() -> validator.validate(single, 60)).doesNotThrowAnyException();
    }

    @Test
    void rejectsPrerequisiteThatDoesNotMatchParent() {
        ExperimentTree valid = tree(false);
        PlanNode branch = valid.nodes().get(1);
        List<PlanNode> nodes = new ArrayList<>(valid.nodes());
        nodes.set(1, new PlanNode(branch.key(), branch.parentKey(), List.of(),
                branch.unlockRule(), branch.completionRule(), branch.depth(), branch.stage(), branch.title(),
                branch.description(), branch.estimatedMinutes(), branch.tasks()));

        assertThatThrownBy(() -> validator.validate(
                new ExperimentTree("0.1", valid.title(), valid.purpose(), nodes), 600))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("前置必须且只能");
    }

    @Test
    void rejectsBlankParentKeyInsteadOfTreatingItAsAnotherRoot() {
        ExperimentTree valid = tree(false);
        PlanNode root = valid.nodes().get(0);
        List<PlanNode> nodes = new ArrayList<>(valid.nodes());
        nodes.set(0, new PlanNode(root.key(), "", root.depth(), root.stage(), root.title(),
                root.description(), root.estimatedMinutes(), root.tasks()));

        assertThatThrownBy(() -> validator.validate(
                new ExperimentTree("0.1", valid.title(), valid.purpose(), nodes), 600))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("必须使用 null");
    }

    @Test
    void rejectsMissingParent() {
        ExperimentTree tree = tree(true);
        assertThatThrownBy(() -> validator.validate(tree, 600))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("父节点不存在");
    }

    @Test
    void rejectsParentCycle() {
        ExperimentTree valid = tree(false);
        List<PlanNode> nodes = new ArrayList<>(valid.nodes());
        PlanNode branch = nodes.get(1);
        nodes.set(1, new PlanNode(branch.key(), "leaf-1", branch.depth(), branch.stage(), branch.title(),
                branch.description(), branch.estimatedMinutes(), branch.tasks()));
        ExperimentTree cyclic = new ExperimentTree("0.1", valid.title(), valid.purpose(), nodes);

        assertThatThrownBy(() -> validator.validate(cyclic, 600))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("环路");
    }

    @Test
    void rejectsVagueTaskAndInvalidMinutes() {
        ExperimentTree valid = tree(false);
        List<PlanNode> nodes = new ArrayList<>(valid.nodes());
        PlanNode root = nodes.get(0);
        List<PlanTask> vague = List.of(
                new PlanTask("学习 Spring", "阅读内容", "manual", Map.of()),
                new PlanTask("实现功能", "完成实现", "manual", Map.of()),
                new PlanTask("验证结果", "运行检查", "test", Map.of()));
        nodes.set(0, new PlanNode(root.key(), null, 0, root.stage(), root.title(),
                root.description(), -1, vague));

        assertThatThrownBy(() -> validator.validate(
                new ExperimentTree("0.1", valid.title(), valid.purpose(), nodes), 600))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("具体动作")
                .hasMessageContaining("预计工时");
    }

    private ExperimentTree tree(boolean missingParent) {
        List<PlanNode> nodes = new ArrayList<>();
        nodes.add(node("root", null, 0));
        for (int index = 1; index <= 3; index++) {
            nodes.add(node("branch-" + index, "root", 1));
        }
        for (int index = 1; index <= 6; index++) {
            String parent = missingParent && index == 1 ? "missing" : "branch-" + (((index - 1) % 3) + 1);
            nodes.add(node("leaf-" + index, parent, 2));
        }
        return new ExperimentTree("0.1", "树状计划", List.of("完成树状实验"), nodes);
    }

    private PlanNode node(String key, String parent, int depth) {
        List<PlanTask> tasks = List.of(
                new PlanTask("创建项目结构", "创建可运行结构", "manual", Map.of()),
                new PlanTask("实现目标功能", "完成节点功能", "manual", Map.of()),
                new PlanTask("验证节点结果", "运行检查确认结果", "test", Map.of()));
        return new PlanNode(key, parent, depth, "阶段 " + depth, "节点 " + key,
                "具体节点说明", 30, tasks);
    }
}
