package com.bmhs.experimentcreation;

import com.bmhs.experimentcreation.CreationModels.ExperimentTree;
import com.bmhs.experimentcreation.CreationModels.PlanNode;
import com.bmhs.experimentcreation.CreationModels.PositionedNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class TreeLayoutService {
    private static final double X_GAP = 320;
    private static final double Y_GAP = 250;

    public List<PositionedNode> layout(ExperimentTree tree) {
        Map<String, List<PlanNode>> children = new HashMap<>();
        PlanNode root = null;
        for (PlanNode node : tree.nodes()) {
            if (node.parentKey() == null || node.parentKey().isBlank()) root = node;
            else children.computeIfAbsent(node.parentKey(), ignored -> new ArrayList<>()).add(node);
        }
        children.values().forEach(nodes -> nodes.sort(Comparator.comparing(PlanNode::key)));

        Map<String, Double> rawX = new LinkedHashMap<>();
        double[] nextLeaf = {0};
        assignX(root, children, rawX, nextLeaf);
        double rootX = rawX.getOrDefault(root.key(), 0d);

        List<PositionedNode> positioned = new ArrayList<>();
        tree.nodes().stream()
                .sorted(Comparator.comparingInt(PlanNode::depth).thenComparing(PlanNode::key))
                .forEach(node -> positioned.add(new PositionedNode(
                        node.key(), node.parentKey(), node.prerequisiteKeys(), node.unlockRule(), node.completionRule(),
                        node.depth(), node.stage(), node.title(), node.description(), node.estimatedMinutes(), node.tasks(),
                        BigDecimal.valueOf(rawX.get(node.key()) - rootX),
                        BigDecimal.valueOf(node.depth() * Y_GAP))));
        return positioned;
    }

    private double assignX(PlanNode node, Map<String, List<PlanNode>> children,
                           Map<String, Double> rawX, double[] nextLeaf) {
        List<PlanNode> descendants = children.getOrDefault(node.key(), List.of());
        double x;
        if (descendants.isEmpty()) {
            x = nextLeaf[0]++ * X_GAP;
        } else {
            double first = assignX(descendants.get(0), children, rawX, nextLeaf);
            double last = first;
            for (int index = 1; index < descendants.size(); index++) {
                last = assignX(descendants.get(index), children, rawX, nextLeaf);
            }
            x = (first + last) / 2;
        }
        rawX.put(node.key(), x);
        return x;
    }
}
