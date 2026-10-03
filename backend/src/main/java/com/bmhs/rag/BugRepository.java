package com.bmhs.rag;

import com.bmhs.experimentcreation.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class BugRepository {
    private final JdbcTemplate jdbc;

    public BugRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public RagModels.BugCaseView create(long runId, long studentId, RagModels.BugRequest request) {
        RunContext context = findRunContext(runId, studentId);
        if (request.nodeId() != null && !nodeBelongs(context.experimentId(), request.nodeId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "NODE_NOT_IN_EXPERIMENT", "节点不属于当前实验");
        }
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO bug_cases
                      (experiment_id, run_id, node_id, course_id, student_id, title, problem, solution, technology_stack)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, context.experimentId());
            statement.setLong(2, runId);
            if (request.nodeId() == null) statement.setObject(3, null);
            else statement.setLong(3, request.nodeId());
            if (context.courseId() == null) statement.setObject(4, null);
            else statement.setLong(4, context.courseId());
            statement.setLong(5, studentId);
            statement.setString(6, request.title().trim());
            statement.setString(7, request.problem().trim());
            statement.setString(8, request.solution() == null ? null : request.solution().trim());
            statement.setString(9, normalizeStack(request.technologyStack()));
            return statement;
        }, keys);
        return findById(requiredKey(keys));
    }

    public List<RagModels.BugCaseView> findVisibleForStudent(long studentId) {
        return jdbc.query("""
                SELECT b.id, b.experiment_id, b.run_id, b.node_id, b.course_id, b.student_id,
                       b.title, b.problem, b.solution, b.technology_stack, b.status, b.visibility, b.vector_status,
                       b.chroma_document_id, b.created_at, b.updated_at
                FROM bug_cases b
                WHERE b.student_id = ?
                   OR (b.status = 'approved' AND b.course_id IN (
                       SELECT cm.course_id FROM course_members cm
                       WHERE cm.student_id = ? AND cm.status = 'active'))
                ORDER BY b.updated_at DESC
                """, this::toView, studentId, studentId);
    }

    public List<RagModels.BugCaseView> findForTeacher(long teacherId, long courseId) {
        assertTeacherOwnsCourse(teacherId, courseId);
        return jdbc.query("""
                SELECT b.id, b.experiment_id, b.run_id, b.node_id, b.course_id, b.student_id,
                       b.title, b.problem, b.solution, b.technology_stack, b.status, b.visibility, b.vector_status,
                       b.chroma_document_id, b.created_at, b.updated_at
                FROM bug_cases b
                WHERE b.course_id = ? ORDER BY b.updated_at DESC
                """, this::toView, courseId);
    }

    public RagModels.BugCaseView findById(long bugCaseId) {
        List<RagModels.BugCaseView> rows = jdbc.query("""
                SELECT id, experiment_id, run_id, node_id, course_id, student_id,
                       title, problem, solution, technology_stack, status, visibility, vector_status,
                       chroma_document_id, created_at, updated_at
                FROM bug_cases WHERE id = ?
                """, this::toView, bugCaseId);
        if (rows.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "BUG_CASE_NOT_FOUND", "Bug 案例不存在");
        return rows.get(0);
    }

    public RagModels.BugCaseView findVisibleByStudent(long bugCaseId, long studentId) {
        List<RagModels.BugCaseView> rows = jdbc.query("""
                SELECT b.id, b.experiment_id, b.run_id, b.node_id, b.course_id, b.student_id,
                       b.title, b.problem, b.solution, b.technology_stack, b.status, b.visibility, b.vector_status,
                       b.chroma_document_id, b.created_at, b.updated_at
                FROM bug_cases b
                WHERE b.id = ? AND (b.student_id = ? OR
                  (b.status = 'approved' AND b.course_id IN
                    (SELECT cm.course_id FROM course_members cm WHERE cm.student_id = ? AND cm.status = 'active')))
                """, this::toView, bugCaseId, studentId, studentId);
        if (rows.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "BUG_CASE_NOT_FOUND", "Bug 案例不存在或无权访问");
        return rows.get(0);
    }

    @Transactional
    public RagModels.BugCaseView review(long bugCaseId, long teacherId, RagModels.BugReviewRequest request) {
        RagModels.BugCaseView bug = findById(bugCaseId);
        if (bug.courseId() == null) throw new ApiException(HttpStatus.CONFLICT, "BUG_CASE_NO_COURSE", "个人案例不能进入课程知识库");
        assertTeacherOwnsCourse(teacherId, bug.courseId());
        String status = request.decision();
        String visibility = "approved".equals(status) ? "course" : "personal";
        jdbc.update("UPDATE bug_cases SET status = ?, visibility = ?, vector_status = 'not_indexed', chroma_document_id = NULL WHERE id = ?",
                status, visibility, bugCaseId);
        jdbc.update("INSERT INTO bug_case_reviews (bug_case_id, teacher_id, decision, feedback) VALUES (?, ?, ?, ?)",
                bugCaseId, teacherId, status, request.feedback());
        return findById(bugCaseId);
    }

    public void updateVectorStatus(long bugCaseId, String status, String documentId) {
        jdbc.update("UPDATE bug_cases SET vector_status = ?, chroma_document_id = ? WHERE id = ?",
                status, documentId, bugCaseId);
    }

    public Long findCourseIdForRun(long runId, long studentId) {
        List<Long> rows = jdbc.query("""
                SELECT a.course_id
                FROM experiment_runs r
                JOIN course_experiment_assignments a ON a.experiment_id = r.experiment_id AND a.status = 'published'
                JOIN course_members m ON m.course_id = a.course_id AND m.student_id = r.student_id AND m.status = 'active'
                WHERE r.id = ? AND r.student_id = ? AND EXISTS (
                  SELECT 1 FROM courses c WHERE c.id = a.course_id AND c.status = 'active')
                LIMIT 1
                """, (rs, rowNum) -> rs.getLong(1), runId, studentId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private RunContext findRunContext(long runId, long studentId) {
        List<RunContext> rows = jdbc.query("""
                SELECT r.experiment_id,
                  (SELECT a.course_id FROM course_experiment_assignments a
                   JOIN course_members m ON m.course_id = a.course_id AND m.student_id = r.student_id AND m.status = 'active'
                   WHERE a.experiment_id = r.experiment_id AND a.status = 'published' LIMIT 1) AS course_id
                FROM experiment_runs r WHERE r.id = ? AND r.student_id = ?
                """, (rs, rowNum) -> new RunContext(rs.getLong("experiment_id"), rs.getObject("course_id", Long.class)),
                runId, studentId);
        if (rows.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "RUN_NOT_FOUND", "运行实例不存在");
        return rows.get(0);
    }

    private void assertTeacherOwnsCourse(long teacherId, long courseId) {
        Integer found = jdbc.query("SELECT 1 FROM courses WHERE id = ? AND teacher_id = ? AND status = 'active'",
                rs -> rs.next() ? 1 : null, courseId, teacherId);
        if (found == null) throw new ApiException(HttpStatus.FORBIDDEN, "COURSE_FORBIDDEN", "无权访问该课程案例");
    }

    private boolean nodeBelongs(long experimentId, long nodeId) {
        Integer found = jdbc.query("SELECT 1 FROM experiment_nodes WHERE id = ? AND experiment_id = ?",
                rs -> rs.next() ? 1 : null, nodeId, experimentId);
        return found != null;
    }

    private RagModels.BugCaseView toView(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new RagModels.BugCaseView(rs.getLong("id"), rs.getLong("experiment_id"), rs.getLong("run_id"),
                rs.getObject("node_id", Long.class), rs.getObject("course_id", Long.class), rs.getLong("student_id"),
                rs.getString("title"), rs.getString("problem"), rs.getString("solution"), rs.getString("technology_stack"), rs.getString("status"),
                rs.getString("visibility"), rs.getString("vector_status"), rs.getString("chroma_document_id"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    private String normalizeStack(String technologyStack) {
        return technologyStack == null || technologyStack.isBlank() ? null : technologyStack.trim();
    }

    private long requiredKey(KeyHolder keys) {
        Number key = keys.getKey();
        if (key == null) throw new IllegalStateException("数据库未返回 Bug 案例主键");
        return key.longValue();
    }

    private record RunContext(long experimentId, Long courseId) {}
}
