package com.bmhs.experimentcreation;

import com.bmhs.experimentcreation.CreationModels.ConfirmedIntent;
import com.bmhs.experimentcreation.CreationModels.Difficulty;
import com.bmhs.experimentcreation.CreationModels.PlanResponse;
import com.bmhs.experimentcreation.CreationModels.PositionedNode;
import com.bmhs.experimentcreation.CreationModels.StudentIntentEnvelope;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Repository
public class CreationRepository {
    public record SessionRecord(
            String id,
            long userId,
            String status,
            StudentIntentEnvelope direction,
            ConfirmedIntent confirmedIntent,
            int intentVersion,
            String intentHash,
            Integer durationMinutes,
            Difficulty difficulty,
            String materializationKey,
            Long experimentId,
            PlanResponse plan) {}

    private final JdbcTemplate jdbc;
    private final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    public CreationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public SessionRecord create(long userId) {
        String id = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO experiment_creation_sessions (id, user_id) VALUES (?, ?)", id, userId);
        return find(id, userId);
    }

    public SessionRecord find(String sessionId, long userId) {
        return findWithSql(sessionId, userId, "");
    }

    public SessionRecord lock(String sessionId, long userId) {
        return findWithSql(sessionId, userId, " FOR UPDATE");
    }

    private SessionRecord findWithSql(String sessionId, long userId, String suffix) {
        try {
            String sql = """
                    SELECT id, user_id, status, direction_envelope, confirmed_intent, intent_version,
                           intent_hash, duration_minutes, difficulty, materialization_key, created_experiment_id
                    FROM experiment_creation_sessions WHERE id = ? AND user_id = ?
                    """ + suffix;
            SessionRecord base = jdbc.queryForObject(sql, sessionMapper(), sessionId, userId);
            PlanResponse plan = findLatestPlan(sessionId);
            return new SessionRecord(base.id(), base.userId(), base.status(), base.direction(), base.confirmedIntent(),
                    base.intentVersion(), base.intentHash(), base.durationMinutes(), base.difficulty(),
                    base.materializationKey(), base.experimentId(), plan);
        } catch (EmptyResultDataAccessException exception) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "创建会话不存在或不属于当前用户");
        }
    }

    private RowMapper<SessionRecord> sessionMapper() {
        return (rs, rowNum) -> {
            String directionJson = rs.getString("direction_envelope");
            String confirmedJson = rs.getString("confirmed_intent");
            String difficulty = rs.getString("difficulty");
            Object duration = rs.getObject("duration_minutes");
            Object experimentId = rs.getObject("created_experiment_id");
            return new SessionRecord(
                    rs.getString("id"), rs.getLong("user_id"), rs.getString("status"),
                    directionJson == null ? null : gson.fromJson(directionJson, StudentIntentEnvelope.class),
                    confirmedJson == null ? null : gson.fromJson(confirmedJson, ConfirmedIntent.class),
                    rs.getInt("intent_version"), rs.getString("intent_hash"),
                    duration == null ? null : ((Number) duration).intValue(),
                    difficulty == null ? null : Difficulty.valueOf(difficulty.toUpperCase(Locale.ROOT)),
                    rs.getString("materialization_key"),
                    experimentId == null ? null : ((Number) experimentId).longValue(), null);
        };
    }

    public void appendMessage(String sessionId, String role, String content) {
        jdbc.update("INSERT INTO experiment_creation_messages (session_id, role, content) VALUES (?, ?, ?)",
                sessionId, role, content);
    }

    public void saveDirection(String sessionId, StudentIntentEnvelope envelope) {
        String status = "ready_for_confirmation".equals(envelope.status()) ? "direction_ready" : "direction_discussion";
        jdbc.update("""
                UPDATE experiment_creation_sessions
                SET direction_envelope = ?, status = ?, intent_version = intent_version + 1,
                    confirmed_intent = NULL, intent_hash = NULL, duration_minutes = NULL, difficulty = NULL
                WHERE id = ?
                """, gson.toJson(envelope), status, sessionId);
    }

    public void confirmDirection(String sessionId, StudentIntentEnvelope confirmed, ConfirmedIntent intent, String hash) {
        jdbc.update("""
                UPDATE experiment_creation_sessions
                SET direction_envelope = ?, confirmed_intent = ?, intent_hash = ?, status = 'direction_confirmed'
                WHERE id = ?
                """, gson.toJson(confirmed), gson.toJson(intent), hash, sessionId);
    }

    public void configure(String sessionId, int durationMinutes, Difficulty difficulty) {
        jdbc.update("""
                UPDATE experiment_creation_sessions
                SET duration_minutes = ?, difficulty = ?, status = 'configured'
                WHERE id = ?
                """, durationMinutes, difficulty.name().toLowerCase(Locale.ROOT), sessionId);
    }

    public void markPlanning(String sessionId) {
        jdbc.update("UPDATE experiment_creation_sessions SET status = 'planning' WHERE id = ?", sessionId);
    }

    public PlanResponse savePlan(String sessionId, String intentHash, PlanResponse plan) {
        Integer nextVersion = jdbc.queryForObject(
                "SELECT COALESCE(MAX(version), 0) + 1 FROM experiment_plan_drafts WHERE session_id = ?",
                Integer.class, sessionId);
        jdbc.update("UPDATE experiment_plan_drafts SET status = 'superseded' WHERE session_id = ? AND status = 'ready'", sessionId);
        String id = UUID.randomUUID().toString();
        PlanResponse versioned = new PlanResponse(id, nextVersion, intentHash, plan.title(), plan.purpose(), plan.nodes());
        jdbc.update("""
                INSERT INTO experiment_plan_drafts (id, session_id, version, intent_hash, plan_json)
                VALUES (?, ?, ?, ?, ?)
                """, id, sessionId, nextVersion, intentHash, gson.toJson(versioned));
        jdbc.update("UPDATE experiment_creation_sessions SET status = 'plan_ready' WHERE id = ?", sessionId);
        return versioned;
    }

    private PlanResponse findLatestPlan(String sessionId) {
        List<String> rows = jdbc.query("""
                SELECT plan_json FROM experiment_plan_drafts
                WHERE session_id = ? AND status IN ('ready', 'accepted')
                ORDER BY version DESC LIMIT 1
                """, (rs, rowNum) -> rs.getString(1), sessionId);
        return rows.isEmpty() ? null : gson.fromJson(rows.get(0), PlanResponse.class);
    }

    public long insertExperiment(long userId, String name, String learningGoal, String description,
                                 Difficulty difficulty, int durationMinutes, String generationConfig) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO experiments
                      (creator_id, name, learning_goal, description, difficulty, duration_days,
                       duration_minutes, status, visibility, generation_config)
                    VALUES (?, ?, ?, ?, ?, ?, ?, 'draft', 'private', ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, userId);
            statement.setString(2, name);
            statement.setString(3, learningGoal);
            statement.setString(4, description);
            statement.setString(5, difficulty.name().toLowerCase(Locale.ROOT));
            statement.setInt(6, Math.max(1, (int) Math.ceil(durationMinutes / 1440.0)));
            statement.setInt(7, durationMinutes);
            statement.setString(8, generationConfig);
            return statement;
        }, keys);
        return requiredKey(keys);
    }

    public long insertStage(long experimentId, String name, int sortOrder) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO experiment_stages (experiment_id, name, sort_order) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, experimentId);
            statement.setString(2, name);
            statement.setInt(3, sortOrder);
            return statement;
        }, keys);
        return requiredKey(keys);
    }

    public long insertNode(long experimentId, Long stageId, Long parentId, PositionedNode node, int sortOrder) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO experiment_nodes
                      (experiment_id, stage_id, parent_node_id, sequence_code, name, description,
                       estimated_minutes, canvas_x, canvas_y, sort_order)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, experimentId);
            if (stageId == null) statement.setNull(2, java.sql.Types.BIGINT); else statement.setLong(2, stageId);
            if (parentId == null) statement.setNull(3, java.sql.Types.BIGINT); else statement.setLong(3, parentId);
            statement.setString(4, node.key());
            statement.setString(5, node.title());
            statement.setString(6, node.description());
            statement.setInt(7, node.estimatedMinutes());
            statement.setBigDecimal(8, node.canvasX());
            statement.setBigDecimal(9, node.canvasY());
            statement.setInt(10, sortOrder);
            return statement;
        }, keys);
        return requiredKey(keys);
    }

    public void insertDependency(long experimentId, long parentId, long childId) {
        jdbc.update("""
                INSERT INTO node_dependencies (experiment_id, from_node_id, to_node_id, relation_type)
                VALUES (?, ?, ?, 'prerequisite')
                """, experimentId, parentId, childId);
    }

    public void insertTask(long experimentId, long nodeId, CreationModels.PlanTask task, int sortOrder) {
        String validationType = task.validationType() == null ? "manual" : task.validationType();
        jdbc.update("""
                INSERT INTO node_tasks
                  (experiment_id, node_id, title, description, sort_order, validation_type, validation_config)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, experimentId, nodeId, task.title(), task.description(), sortOrder,
                validationType, task.validationConfig() == null ? null : gson.toJson(task.validationConfig()));
    }

    public void finishMaterialization(String sessionId, String planId, String idempotencyKey, long experimentId) {
        jdbc.update("UPDATE experiment_plan_drafts SET status = 'accepted' WHERE id = ? AND session_id = ?",
                planId, sessionId);
        jdbc.update("""
                UPDATE experiment_creation_sessions
                SET status = 'materialized', materialization_key = ?, created_experiment_id = ?
                WHERE id = ?
                """, idempotencyKey, experimentId, sessionId);
    }

    private long requiredKey(KeyHolder keys) {
        Number key = keys.getKey();
        if (key == null) throw new IllegalStateException("数据库未返回生成的主键");
        return key.longValue();
    }
}
