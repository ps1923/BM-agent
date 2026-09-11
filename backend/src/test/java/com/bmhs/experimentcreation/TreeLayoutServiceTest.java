package com.bmhs.experimentcreation;

import com.bmhs.experimentcreation.CreationModels.ExperimentTree;
import com.bmhs.experimentcreation.CreationModels.PlanNode;
import com.bmhs.experimentcreation.CreationModels.PlanTask;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TreeLayoutServiceTest {
    @Test
    void centersParentsAndSpreadsChildren() {
        PlanTask task = new PlanTask("创建功能", "执行具体动作", "manual", Map.of());
        List<PlanTask> tasks = List.of(task, task, task);
        ExperimentTree tree = new ExperimentTree("0.1", "布局", List.of("展示"), List.of(
                new PlanNode("root", null, 0, "根", "根", "根", 10, tasks),
                new PlanNode("a", "root", 1, "分支", "A", "A", 10, tasks),
                new PlanNode("b", "root", 1, "分支", "B", "B", 10, tasks)));

        var nodes = new TreeLayoutService().layout(tree);
        assertThat(nodes).extracting(node -> node.canvasY().intValue()).containsExactly(0, 250, 250);
        assertThat(nodes.get(0).canvasX()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(nodes.get(1).canvasX()).isLessThan(nodes.get(0).canvasX());
        assertThat(nodes.get(2).canvasX()).isGreaterThan(nodes.get(0).canvasX());
    }
}
