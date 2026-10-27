package com.bmhs.teacher;

import com.bmhs.experimentcreation.ApiException;
import com.bmhs.experimentcreation.CreationModels.AcceptanceCriterion;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.reflect.TypeToken;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Type;
import java.util.List;

@Repository
public class NodeAcceptanceCriteriaRepository {
    private static final Type CRITERIA_TYPE = new TypeToken<List<AcceptanceCriterion>>() {}.getType();
    private record NodeCriteriaRow(long nodeId, String code, String name, Integer version, String source, String criteriaJson) {}
    private final JdbcTemplate jdbc;
    private final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    public NodeAcceptanceCriteriaRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<NodeAcceptanceCriteriaModels.RevisionView> history(long experimentId, long nodeId) {
        return jdbc.query("""
                SELECT version, source, criteria_json, updated_by, created_at
                FROM experiment_node_acceptance_criteria
                WHERE experiment_id = ? AND node_id = ? ORDER BY version DESC
                """, (rs, rowNum) -> new NodeAcceptanceCriteriaModels.RevisionView(experimentId, nodeId,
                rs.getInt("version"), rs.getString("source"), gson.fromJson(rs.getString("criteria_json"), CRITERIA_TYPE),
                rs.getObject("updated_by", Long.class), rs.getTimestamp("created_at").toInstant()), experimentId, nodeId);
    }

    public List<NodeAcceptanceCriteriaModels.NodeView> currentForExperiment(long experimentId) {
        List<NodeCriteriaRow> rows = jdbc.query("""
                SELECT n.id, n.sequence_code, n.name, n.description, c.version, c.source, c.criteria_json
                FROM experiment_nodes n
                LEFT JOIN experiment_node_acceptance_criteria c ON c.experiment_id = n.experiment_id
                  AND c.node_id = n.id AND c.version = (
                    SELECT MAX(latest.version) FROM experiment_node_acceptance_criteria latest
                    WHERE latest.experiment_id = n.experiment_id AND latest.node_id = n.id)
                WHERE n.experiment_id = ? ORDER BY n.sort_order, n.id
                """, (rs, rowNum) -> new NodeCriteriaRow(rs.getLong("id"), rs.getString("sequence_code"),
                rs.getString("name"), rs.getObject("version", Integer.class), rs.getString("source"),
                rs.getString("criteria_json")), experimentId);
        List<NodeAcceptanceCriteriaModels.NodeView> result = new java.util.ArrayList<>();
        for (NodeCriteriaRow row : rows) {
            List<AcceptanceCriterion> criteria = row.criteriaJson() == null ? List.of()
                    : gson.fromJson(row.criteriaJson(), CRITERIA_TYPE);
            if (criteria == null) criteria = List.of();
            if (criteria.isEmpty()) {
                criteria = jdbc.query("SELECT title, description, validation_type FROM node_tasks WHERE node_id = ? ORDER BY sort_order, id",
                        (taskRs, taskRow) -> new AcceptanceCriterion(taskRs.getString("title"),
                                "manual".equals(taskRs.getString("validation_type")) ? "manual_demo"
                                        : "agent".equals(taskRs.getString("validation_type")) ? "static_code" : "test_artifact",
                                taskRs.getString("description")), row.nodeId());
            }
            result.add(new NodeAcceptanceCriteriaModels.NodeView(experimentId, row.nodeId(), row.code(), row.name(),
                    row.version() == null ? 0 : row.version(),
                    row.source() == null ? "legacy_inferred" : row.source(), criteria));
        }
        return List.copyOf(result);
    }

    @Transactional
    public NodeAcceptanceCriteriaModels.RevisionView append(long experimentId, long nodeId, long teacherId,
                                                              List<AcceptanceCriterion> criteria) {
        Integer nodeExists = jdbc.query("SELECT 1 FROM experiment_nodes WHERE id = ? AND experiment_id = ? FOR UPDATE",
                rs -> rs.next() ? 1 : null, nodeId, experimentId);
        if (nodeExists == null) throw new ApiException(HttpStatus.NOT_FOUND, "EXPERIMENT_NODE_NOT_FOUND", "实验节点不存在");
        Integer version = jdbc.queryForObject("""
                SELECT COALESCE(MAX(version), 0) + 1 FROM experiment_node_acceptance_criteria
                WHERE experiment_id = ? AND node_id = ?
                """, Integer.class, experimentId, nodeId);
        jdbc.update("""
                INSERT INTO experiment_node_acceptance_criteria
                  (experiment_id, node_id, version, source, criteria_json, updated_by)
                VALUES (?, ?, ?, 'teacher', ?, ?)
                """, experimentId, nodeId, version, gson.toJson(criteria), teacherId);
        return history(experimentId, nodeId).stream().filter(item -> item.version() == version).findFirst()
                .orElseThrow(() -> new IllegalStateException("验收准则新版本保存后无法读取"));
    }

    public void insertGenerated(long experimentId, long nodeId, List<AcceptanceCriterion> criteria) {
        jdbc.update("""
                INSERT INTO experiment_node_acceptance_criteria
                  (experiment_id, node_id, version, source, criteria_json)
                VALUES (?, ?, 1, 'generated', ?)
                """, experimentId, nodeId, gson.toJson(criteria));
    }
}
