package com.bmhs.experimentcreation;

import com.bmhs.experimentcreation.CreationModels.ExperimentTree;
import com.bmhs.experimentcreation.CreationModels.PlanNode;
import com.bmhs.experimentcreation.CreationModels.PlanTask;
import com.bmhs.experimentcreation.CreationModels.PositionedTask;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TreeLayoutServiceTest {
    @Test
    void generatedPlanTasksKeepTheirOriginalFieldsAndHaveNoDatabaseId() {
        PlanTask task = new PlanTask("创建入口类", "添加启动类", "manual", Map.of());
        PlanNode root = new PlanNode("01", null, 0, "阶段一", "初始化", "创建项目结构", 30, List.of(task));
        ExperimentTree tree = new ExperimentTree("0.1", "Java 实验", List.of("完成接口"), List.of(root));

        PositionedTask positioned = new TreeLayoutService().layout(tree).get(0).tasks().get(0);

        assertNull(positioned.taskId());
        assertEquals(task.title(), positioned.title());
        assertEquals(task.validationType(), positioned.validationType());
    }
}
