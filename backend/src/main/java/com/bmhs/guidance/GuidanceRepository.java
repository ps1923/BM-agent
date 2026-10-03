package com.bmhs.guidance;

import com.bmhs.experimentcreation.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class GuidanceRepository {
    private final JdbcTemplate jdbc;

    public GuidanceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public GuidanceModels.ExperimentContext findContext(long runId, long studentId) {
        List<GuidanceModels.ExperimentContext> rows = jdbc.query("""
                SELECT r.experiment_id, r.id AS run_id, e.name, e.learning_goal, e.description,
                       n.id AS node_id, n.name AS node_name, n.description AS node_description
                FROM experiment_runs r
                JOIN experiments e ON e.id = r.experiment_id
                LEFT JOIN experiment_nodes n ON n.id = r.current_node_id AND n.experiment_id = r.experiment_id
                WHERE r.id = ? AND r.student_id = ?
                """, (rs, rowNum) -> new GuidanceModels.ExperimentContext(
                rs.getLong("experiment_id"), rs.getLong("run_id"), rs.getString("name"),
                rs.getString("learning_goal"), rs.getString("description"),
                rs.getObject("node_id", Long.class), rs.getString("node_name"),
                rs.getString("node_description")), runId, studentId);
        if (rows.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "RUN_NOT_FOUND", "运行实例不存在");
        return rows.get(0);
    }

    public List<GuidanceModels.ChatMessage> recentMessages(long runId, long experimentId, int limit) {
        List<GuidanceModels.ChatMessage> messages = jdbc.query("""
                SELECT role, content
                FROM chat_messages
                WHERE run_id = ? AND experiment_id = ?
                ORDER BY id DESC
                LIMIT ?
                """, (rs, rowNum) -> new GuidanceModels.ChatMessage(rs.getString("role"), rs.getString("content")),
                runId, experimentId, limit);
        Collections.reverse(messages);
        return messages;
    }

    public void appendMessage(long runId, long experimentId, Long nodeId, String role,
                              String messageType, String content) {
        jdbc.update("""
                INSERT INTO chat_messages (experiment_id, run_id, node_id, role, message_type, content)
                VALUES (?, ?, ?, ?, ?, ?)
                """, experimentId, runId, nodeId, role, messageType, content);
    }
}
