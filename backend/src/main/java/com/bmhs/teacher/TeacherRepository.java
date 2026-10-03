package com.bmhs.teacher;

import com.bmhs.experimentcreation.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public class TeacherRepository {
    private final JdbcTemplate jdbc;

    public TeacherRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public TeacherModels.CourseDashboard dashboard(long teacherId, long courseId) {
        CourseRow course = findCourseOwned(teacherId, courseId);
        List<TeacherModels.StudentProgress> students = jdbc.query("""
                SELECT u.id AS student_id, u.display_name, u.email, r.id AS run_id,
                       r.experiment_id, e.name AS experiment_name, r.status AS run_status,
                       COALESCE(SUM(CASE WHEN np.status = 'completed' THEN 1 ELSE 0 END), 0) AS completed_nodes,
                       COUNT(np.id) AS total_nodes,
                       COALESCE((SELECT COUNT(*) FROM task_progress tp
                                 WHERE tp.run_id = r.id AND tp.status = 'completed'), 0) AS completed_tasks,
                       COALESCE((SELECT COUNT(*) FROM task_progress tp WHERE tp.run_id = r.id), 0) AS total_tasks,
                       r.last_opened_at
                FROM course_members m
                JOIN users u ON u.id = m.student_id
                LEFT JOIN course_experiment_assignments a ON a.course_id = m.course_id AND a.status = 'published'
                LEFT JOIN experiments e ON e.id = a.experiment_id
                LEFT JOIN experiment_runs r ON r.experiment_id = a.experiment_id AND r.student_id = m.student_id
                LEFT JOIN node_progress np ON np.run_id = r.id AND np.experiment_id = r.experiment_id
                WHERE m.course_id = ? AND m.status = 'active'
                GROUP BY u.id, u.display_name, u.email, r.id, r.experiment_id, e.id, e.name, r.status, r.last_opened_at
                ORDER BY r.last_opened_at DESC, u.id, e.id
                """, (rs, rowNum) -> new TeacherModels.StudentProgress(
                rs.getLong("student_id"), rs.getString("display_name"), rs.getString("email"),
                rs.getObject("run_id", Long.class) == null ? 0L : rs.getLong("run_id"),
                rs.getObject("experiment_id", Long.class) == null ? 0L : rs.getLong("experiment_id"),
                rs.getString("experiment_name"),
                rs.getString("run_status") == null ? "not_started" : rs.getString("run_status"),
                rs.getInt("completed_nodes"), rs.getInt("total_nodes"), rs.getInt("completed_tasks"),
                rs.getInt("total_tasks"), nullableInstant(rs.getTimestamp("last_opened_at"))), courseId);
        Integer memberCount = jdbc.queryForObject("SELECT COUNT(*) FROM course_members WHERE course_id = ? AND status = 'active'",
                Integer.class, courseId);
        Integer experimentCount = jdbc.queryForObject("SELECT COUNT(*) FROM course_experiment_assignments WHERE course_id = ? AND status = 'published'",
                Integer.class, courseId);
        return new TeacherModels.CourseDashboard(courseId, course.name(), memberCount == null ? 0 : memberCount,
                experimentCount == null ? 0 : experimentCount, students);
    }

    public List<TeacherModels.TimelineEvent> timeline(long teacherId, long runId) {
        RunAccess access = assertRunAccess(teacherId, runId);
        List<TeacherModels.TimelineEvent> events = jdbc.query("""
                SELECT * FROM (
                  SELECT * FROM (
                SELECT 'node' AS source, n.name AS title, np.status AS detail,
                       COALESCE(np.completed_at, np.started_at, np.updated_at) AS created_at
                FROM node_progress np JOIN experiment_nodes n ON n.id = np.node_id AND n.experiment_id = np.experiment_id
                WHERE np.run_id = ?
                UNION ALL
                SELECT 'command', '终端命令', cr.status, cr.created_at
                FROM command_runs cr JOIN terminal_sessions ts ON ts.id = cr.terminal_session_id
                JOIN coding_workspaces w ON w.id = ts.workspace_id
                WHERE w.run_id = ?
                UNION ALL
                SELECT 'assistant', role, message_type, created_at
                FROM chat_messages WHERE run_id = ?
                UNION ALL
                SELECT 'bug', title, status, created_at
                FROM bug_cases WHERE run_id = ?
                UNION ALL
                SELECT 'task', nt.title, tp.status, COALESCE(tp.completed_at, tp.updated_at)
                FROM task_progress tp
                JOIN node_tasks nt ON nt.id = tp.task_id AND nt.experiment_id = tp.experiment_id
                WHERE tp.run_id = ? AND tp.status <> 'pending'
                  ) AS all_events
                  ORDER BY created_at DESC
                  LIMIT 500
                ) AS recent_events
                ORDER BY created_at ASC
                """, (rs, rowNum) -> new TeacherModels.TimelineEvent(rs.getString("source"),
                rs.getString("title"), rs.getString("detail"), rs.getTimestamp("created_at").toInstant()),
                runId, runId, runId, runId, runId);
        return events;
    }

    public TeacherModels.RunSummary summary(long teacherId, long runId) {
        RunAccess access = assertRunAccess(teacherId, runId);
        TeacherModels.RunSummary base = jdbc.queryForObject("""
                SELECT r.id, r.experiment_id, e.name, r.status,
                       COALESCE(SUM(CASE WHEN np.status = 'completed' THEN 1 ELSE 0 END), 0) AS completed_nodes,
                       COUNT(np.id) AS total_nodes,
                       (SELECT COUNT(*) FROM task_progress tp WHERE tp.run_id = r.id AND tp.status = 'completed') AS completed_tasks,
                       (SELECT COUNT(*) FROM task_progress tp WHERE tp.run_id = r.id) AS total_tasks,
                       (SELECT COUNT(*) FROM command_runs cr JOIN terminal_sessions ts ON ts.id = cr.terminal_session_id
                        JOIN coding_workspaces w ON w.id = ts.workspace_id WHERE w.run_id = r.id) AS command_count,
                       (SELECT COUNT(*) FROM chat_messages cm WHERE cm.run_id = r.id) AS message_count,
                       (SELECT COUNT(*) FROM bug_cases bc WHERE bc.run_id = r.id) AS bug_count
                FROM experiment_runs r JOIN experiments e ON e.id = r.experiment_id
                LEFT JOIN node_progress np ON np.run_id = r.id
                WHERE r.id = ? GROUP BY r.id, r.experiment_id, e.name, r.status
                """, (rs, rowNum) -> new TeacherModels.RunSummary(rs.getLong("id"), rs.getLong("experiment_id"),
                rs.getString("name"), rs.getString("status"), rs.getInt("completed_nodes"), rs.getInt("total_nodes"),
                rs.getInt("completed_tasks"), rs.getInt("total_tasks"), rs.getInt("command_count"),
                rs.getInt("message_count"), rs.getInt("bug_count"), false, null, null), runId);
        return new TeacherModels.RunSummary(base.runId(), base.experimentId(), base.experimentName(), base.runStatus(),
                base.completedNodes(), base.totalNodes(), base.completedTasks(), base.totalTasks(),
                base.commandCount(), base.messageCount(), base.bugCount(),
                false, null, findReview(teacherId, runId));
    }

    public List<TeacherModels.StageEvidence> stageEvidence(long teacherId, long runId) {
        RunAccess access = assertRunAccess(teacherId, runId);
        return jdbc.query("""
                SELECT n.id AS node_id, n.name AS node_name, s.name AS stage_name, np.status,
                       np.completed_at, ws.id AS snapshot_id, ws.created_at AS snapshot_created_at
                FROM node_progress np
                JOIN experiment_nodes n ON n.id = np.node_id AND n.experiment_id = np.experiment_id
                LEFT JOIN experiment_stages s ON s.id = n.stage_id AND s.experiment_id = n.experiment_id
                LEFT JOIN coding_workspaces w ON w.run_id = np.run_id
                LEFT JOIN workspace_snapshots ws ON ws.workspace_id = w.id AND ws.node_id = n.id
                    AND ws.snapshot_type = 'node_completion'
                WHERE np.run_id = ? AND np.experiment_id = ?
                ORDER BY COALESCE(s.sort_order, 2147483647), n.sort_order, n.id
                """, (rs, rowNum) -> new TeacherModels.StageEvidence(rs.getLong("node_id"),
                rs.getString("node_name"), rs.getString("stage_name"), rs.getString("status"),
                nullableInstant(rs.getTimestamp("completed_at")),
                rs.getObject("snapshot_id", Long.class), nullableInstant(rs.getTimestamp("snapshot_created_at"))),
                runId, access.experimentId());
    }

    SnapshotRecord assertSnapshotAccess(long teacherId, long runId, long snapshotId) {
        assertRunAccess(teacherId, runId);
        List<SnapshotRecord> rows = jdbc.query("""
                SELECT ws.id, ws.node_id, w.storage_path, ws.storage_key
                FROM workspace_snapshots ws
                JOIN coding_workspaces w ON w.id = ws.workspace_id
                JOIN experiment_runs r ON r.id = w.run_id AND r.experiment_id = w.experiment_id
                JOIN experiment_nodes n ON n.id = ws.node_id AND n.experiment_id = r.experiment_id
                WHERE ws.id = ? AND r.id = ? AND ws.snapshot_type = 'node_completion'
                """, (rs, rowNum) -> new SnapshotRecord(rs.getLong("id"), rs.getLong("node_id"),
                rs.getString("storage_path"), rs.getString("storage_key")), snapshotId, runId);
        if (rows.isEmpty()) throw new ApiException(HttpStatus.FORBIDDEN, "SNAPSHOT_FORBIDDEN", "无权查看该阶段代码证据");
        return rows.get(0);
    }

    public TeacherModels.ReviewView review(long teacherId, long runId, TeacherModels.ReviewRequest request) {
        RunAccess access = assertRunAccess(teacherId, runId);
        jdbc.update("""
                INSERT INTO teacher_reviews (course_id, experiment_id, run_id, teacher_id, student_id, rating, feedback)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE rating = VALUES(rating), feedback = VALUES(feedback), updated_at = CURRENT_TIMESTAMP(6)
                """, access.courseId(), access.experimentId(), runId, teacherId, access.studentId(), request.rating(), request.feedback());
        return findReview(teacherId, runId);
    }

    private TeacherModels.ReviewView findReview(long teacherId, long runId) {
        List<TeacherModels.ReviewView> rows = jdbc.query("SELECT id, rating, feedback, updated_at FROM teacher_reviews WHERE run_id = ? AND teacher_id = ?",
                (rs, rowNum) -> new TeacherModels.ReviewView(rs.getLong("id"), rs.getObject("rating", Integer.class), rs.getString("feedback"), rs.getTimestamp("updated_at").toInstant()), runId, teacherId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private CourseRow findCourseOwned(long teacherId, long courseId) {
        List<CourseRow> rows = jdbc.query("SELECT id, name FROM courses WHERE id = ? AND teacher_id = ? AND status = 'active'",
                (rs, rowNum) -> new CourseRow(rs.getLong("id"), rs.getString("name")), courseId, teacherId);
        if (rows.isEmpty()) throw new ApiException(HttpStatus.FORBIDDEN, "COURSE_FORBIDDEN", "无权查看该课程看板");
        return rows.get(0);
    }

    private RunAccess assertRunAccess(long teacherId, long runId) {
        List<RunAccess> rows = jdbc.query("""
                SELECT r.experiment_id, r.student_id, a.course_id
                FROM experiment_runs r
                JOIN course_experiment_assignments a ON a.experiment_id = r.experiment_id AND a.status = 'published'
                JOIN experiments e ON e.id = r.experiment_id AND e.status = 'published'
                JOIN courses c ON c.id = a.course_id AND c.teacher_id = ? AND c.status = 'active'
                JOIN course_members m ON m.course_id = a.course_id AND m.student_id = r.student_id AND m.status = 'active'
                WHERE r.id = ? LIMIT 1
                """, (rs, rowNum) -> new RunAccess(rs.getLong("experiment_id"), rs.getLong("student_id"), rs.getLong("course_id")),
                teacherId, runId);
        if (rows.isEmpty()) throw new ApiException(HttpStatus.FORBIDDEN, "RUN_FORBIDDEN", "无权查看该学生运行实例");
        return rows.get(0);
    }

    private Instant nullableInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private record CourseRow(long id, String name) {}
    private record RunAccess(long experimentId, long studentId, long courseId) {}
    record SnapshotRecord(long id, long nodeId, String storagePath, String storageKey) {}
}
