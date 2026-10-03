package com.bmhs.course;

import com.bmhs.course.CourseModels.CourseMember;
import com.bmhs.course.CourseModels.CourseView;
import com.bmhs.course.CourseModels.ExperimentSummary;
import com.bmhs.course.CourseModels.PublishedExperiment;
import com.bmhs.experimentcreation.CreationModels.MaterializedExperiment;
import com.bmhs.experimentcreation.CreationModels.PositionedTask;
import com.bmhs.experimentcreation.CreationModels.PositionedNode;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.bmhs.experimentcreation.ApiException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Repository
public class CourseRepository {
    private static final String INVITE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public record CourseRow(long id, long teacherId, String name, String inviteCode) {}

    private final JdbcTemplate jdbc;
    private final Gson gson = new Gson();

    public CourseRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public CourseView create(long teacherId, String name) {
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                String inviteCode = inviteCode();
                jdbc.update("INSERT INTO courses (teacher_id, name, invite_code) VALUES (?, ?, ?)",
                        teacherId, name.trim(), inviteCode);
                return findCoursesForTeacher(teacherId).stream()
                        .filter(course -> course.inviteCode().equals(inviteCode))
                        .findFirst()
                        .orElseThrow(() -> new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                                "COURSE_CREATE_FAILED", "课程创建后无法读取，请稍后重试"));
            } catch (DuplicateKeyException duplicate) {
                // Retry only the invite-code collision; the code is intentionally generated server-side.
            }
        }
        throw new ApiException(HttpStatus.CONFLICT, "INVITE_CODE_UNAVAILABLE", "暂时无法生成课程邀请码，请重试");
    }

    public List<CourseView> findCoursesForTeacher(long teacherId) {
        return findCourses("SELECT id, teacher_id, name, invite_code FROM courses "
                + "WHERE teacher_id = ? AND status = 'active' ORDER BY id DESC", teacherId);
    }

    public List<CourseView> findCoursesForStudent(long studentId) {
        return findCourses("SELECT c.id, c.teacher_id, c.name, c.invite_code FROM courses c "
                + "JOIN course_members m ON m.course_id = c.id "
                + "WHERE m.student_id = ? AND m.status = 'active' AND c.status = 'active' "
                + "ORDER BY c.id DESC", studentId);
    }

    public CourseRow findCourse(long courseId) {
        List<CourseRow> rows = jdbc.query("SELECT id, teacher_id, name, invite_code FROM courses "
                        + "WHERE id = ? AND status = 'active'", (rs, rowNum) -> new CourseRow(
                        rs.getLong("id"), rs.getLong("teacher_id"), rs.getString("name"),
                        rs.getString("invite_code")), courseId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public CourseView join(long courseId, long studentId, String inviteCode) {
        CourseRow course = findCourse(courseId);
        if (course == null || !course.inviteCode().equalsIgnoreCase(inviteCode.trim())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "COURSE_NOT_FOUND", "课程邀请码无效或课程不存在");
        }
        Integer existing = jdbc.query("SELECT 1 FROM course_members WHERE course_id = ? AND student_id = ?",
                rs -> rs.next() ? 1 : null, courseId, studentId);
        if (existing != null) {
            jdbc.update("UPDATE course_members SET status = 'active', joined_at = CURRENT_TIMESTAMP(6) "
                    + "WHERE course_id = ? AND student_id = ?", courseId, studentId);
        } else {
            jdbc.update("INSERT INTO course_members (course_id, student_id) VALUES (?, ?)", courseId, studentId);
        }
        return findCoursesForStudent(studentId).stream()
                .filter(item -> item.id() == courseId)
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                        "COURSE_JOIN_FAILED", "加入课程后无法读取课程，请稍后重试"));
    }

    public CourseView joinByInvite(long studentId, String inviteCode) {
        List<CourseRow> courses = jdbc.query("SELECT id, teacher_id, name, invite_code FROM courses "
                        + "WHERE invite_code = ? AND status = 'active'", (rs, rowNum) -> new CourseRow(
                        rs.getLong("id"), rs.getLong("teacher_id"), rs.getString("name"),
                        rs.getString("invite_code")), inviteCode.trim().toUpperCase());
        if (courses.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "COURSE_NOT_FOUND", "课程邀请码无效或课程不存在");
        }
        return join(courses.get(0).id(), studentId, inviteCode);
    }

    public List<CourseMember> findMembers(long courseId) {
        return jdbc.query("SELECT u.id, u.email, u.display_name, m.joined_at "
                        + "FROM course_members m JOIN users u ON u.id = m.student_id "
                        + "WHERE m.course_id = ? AND m.status = 'active' ORDER BY m.joined_at ASC",
                (rs, rowNum) -> new CourseMember(rs.getLong("id"), rs.getString("email"),
                        rs.getString("display_name"), rs.getTimestamp("joined_at").toInstant()), courseId);
    }

    public List<ExperimentSummary> findExperimentsForTeacher(long teacherId) {
        return findExperimentsCreatedBy(teacherId);
    }

    public List<ExperimentSummary> findExperimentsCreatedBy(long userId) {
        return jdbc.query("SELECT id, name, description, status FROM experiments "
                        + "WHERE creator_id = ? AND status <> 'archived' ORDER BY updated_at DESC",
                (rs, rowNum) -> new ExperimentSummary(rs.getLong("id"), rs.getString("name"),
                        rs.getString("description"), rs.getString("status")), userId);
    }

    public PublishedExperiment publish(long teacherId, long experimentId, long courseId) {
        CourseRow course = findCourse(courseId);
        if (course == null || course.teacherId() != teacherId) {
            throw new ApiException(HttpStatus.FORBIDDEN, "COURSE_FORBIDDEN", "只能向自己管理的课程发布实验");
        }
        Integer owned = jdbc.query("SELECT 1 FROM experiments WHERE id = ? AND creator_id = ? "
                        + "AND status <> 'archived'", rs -> rs.next() ? 1 : null, experimentId, teacherId);
        if (owned == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "EXPERIMENT_NOT_FOUND", "实验不存在或不属于当前教师");
        }
        try {
            jdbc.update("UPDATE experiments SET status = 'published', published_at = COALESCE(published_at, "
                    + "CURRENT_TIMESTAMP(6)) WHERE id = ? AND creator_id = ?", experimentId, teacherId);
            jdbc.update("INSERT INTO course_experiment_assignments "
                    + "(course_id, experiment_id, publisher_id) VALUES (?, ?, ?)", courseId, experimentId, teacherId);
        } catch (DuplicateKeyException duplicate) {
            throw new ApiException(HttpStatus.CONFLICT, "EXPERIMENT_ALREADY_PUBLISHED", "该实验已经发布到此课程");
        }
        return jdbc.queryForObject("SELECT e.id, e.name, e.description, a.published_at "
                        + "FROM course_experiment_assignments a JOIN experiments e ON e.id = a.experiment_id "
                        + "WHERE a.course_id = ? AND a.experiment_id = ? AND a.status = 'published'",
                (rs, rowNum) -> new PublishedExperiment(rs.getLong("id"), rs.getString("name"),
                        rs.getString("description"), rs.getTimestamp("published_at").toInstant()),
                courseId, experimentId);
    }

    public MaterializedExperiment findExperiment(long experimentId, long userId) {
        List<MaterializedExperimentHeader> headers = jdbc.query("""
                SELECT e.id, e.name, e.description, e.difficulty, e.duration_minutes
                FROM experiments e
                WHERE e.id = ? AND (
                    e.creator_id = ? OR EXISTS (
                        SELECT 1
                        FROM course_experiment_assignments a
                        JOIN course_members m ON m.course_id = a.course_id
                        WHERE a.experiment_id = e.id AND a.status = 'published'
                          AND m.student_id = ? AND m.status = 'active'
                    )
                )
                """, (rs, rowNum) -> new MaterializedExperimentHeader(rs.getLong("id"),
                rs.getString("name"), rs.getString("description"), rs.getString("difficulty"),
                rs.getInt("duration_minutes")), experimentId, userId, userId);
        if (headers.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "EXPERIMENT_NOT_FOUND", "实验不存在或当前用户无权访问");
        }
        MaterializedExperimentHeader header = headers.get(0);
        List<NodeRow> rows = jdbc.query("""
                SELECT n.id, n.sequence_code, p.sequence_code AS parent_key, s.name AS stage_name,
                       n.name, n.description, n.estimated_minutes, n.canvas_x, n.canvas_y
                FROM experiment_nodes n
                LEFT JOIN experiment_nodes p ON p.id = n.parent_node_id AND p.experiment_id = n.experiment_id
                LEFT JOIN experiment_stages s ON s.id = n.stage_id AND s.experiment_id = n.experiment_id
                WHERE n.experiment_id = ? ORDER BY n.sort_order, n.id
                """, (rs, rowNum) -> new NodeRow(rs.getLong("id"), rs.getString("sequence_code"), rs.getString("parent_key"),
                rs.getString("stage_name"), rs.getString("name"), rs.getString("description"),
                rs.getInt("estimated_minutes"), rs.getBigDecimal("canvas_x"), rs.getBigDecimal("canvas_y")), experimentId);
        Map<String, List<PositionedTask>> tasks = new HashMap<>();
        jdbc.query("""
                SELECT n.sequence_code, t.id, t.title, t.description, t.validation_type, t.validation_config
                FROM node_tasks t JOIN experiment_nodes n ON n.id = t.node_id AND n.experiment_id = t.experiment_id
                WHERE t.experiment_id = ? ORDER BY n.sort_order, t.sort_order, t.id
                """, rs -> {
            while (rs.next()) {
                String config = rs.getString("validation_config");
                Map<String, Object> validationConfig = config == null ? Map.of()
                        : gson.fromJson(config, new TypeToken<Map<String, Object>>() {}.getType());
                tasks.computeIfAbsent(rs.getString("sequence_code"), ignored -> new ArrayList<>())
                        .add(new PositionedTask(rs.getLong("id"), rs.getString("title"), rs.getString("description"),
                                rs.getString("validation_type"), validationConfig == null ? Map.of() : validationConfig));
            }
            return null;
        }, experimentId);
        Map<String, NodeRow> byKey = new HashMap<>();
        rows.forEach(row -> byKey.put(row.key(), row));
        List<PositionedNode> nodes = rows.stream().map(row -> new PositionedNode(row.key(), row.parentKey(),
                depth(row, byKey), row.stageName(), row.name(), row.description(), row.estimatedMinutes(),
                tasks.getOrDefault(row.key(), List.of()), row.canvasX(), row.canvasY(), row.id())).toList();
        return new MaterializedExperiment(header.id(), header.name(), header.description(), header.difficulty(),
                header.durationMinutes(), nodes);
    }

    private int depth(NodeRow row, Map<String, NodeRow> byKey) {
        int depth = 0;
        NodeRow current = row;
        while (current.parentKey() != null && depth < byKey.size()) {
            current = byKey.get(current.parentKey());
            if (current == null) break;
            depth++;
        }
        return depth;
    }

    private record MaterializedExperimentHeader(long id, String name, String description,
                                                String difficulty, int durationMinutes) {}

    private record NodeRow(long id, String key, String parentKey, String stageName, String name, String description,
                           int estimatedMinutes, java.math.BigDecimal canvasX, java.math.BigDecimal canvasY) {}

    private List<CourseView> findCourses(String sql, long userId) {
        List<CourseRow> rows = jdbc.query(sql, (rs, rowNum) -> new CourseRow(rs.getLong("id"),
                rs.getLong("teacher_id"), rs.getString("name"), rs.getString("invite_code")), userId);
        List<CourseView> result = new ArrayList<>();
        for (CourseRow row : rows) {
            Integer memberCount = jdbc.queryForObject("SELECT COUNT(*) FROM course_members "
                    + "WHERE course_id = ? AND status = 'active'", Integer.class, row.id());
            List<PublishedExperiment> experiments = jdbc.query("SELECT e.id, e.name, e.description, a.published_at "
                            + "FROM course_experiment_assignments a JOIN experiments e ON e.id = a.experiment_id "
                            + "WHERE a.course_id = ? AND a.status = 'published' ORDER BY a.published_at DESC",
                    (rs, rowNum) -> new PublishedExperiment(rs.getLong("id"), rs.getString("name"),
                            rs.getString("description"), rs.getTimestamp("published_at").toInstant()), row.id());
            result.add(new CourseView(row.id(), row.teacherId(), row.name(), row.inviteCode(),
                    memberCount == null ? 0 : memberCount, experiments));
        }
        return result;
    }

    private String inviteCode() {
        StringBuilder code = new StringBuilder(8);
        for (int i = 0; i < 8; i++) code.append(INVITE_ALPHABET.charAt(RANDOM.nextInt(INVITE_ALPHABET.length())));
        return code.toString();
    }
}
