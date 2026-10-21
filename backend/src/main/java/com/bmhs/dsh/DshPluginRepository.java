package com.bmhs.dsh;

import com.bmhs.experimentcreation.ApiException;
import com.bmhs.teacher.TeacherModels;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class DshPluginRepository {
    private final JdbcTemplate jdbc;

    public DshPluginRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<DshPluginModels.ExperimentView> experiments(long studentId) {
        return jdbc.query("""
                SELECT DISTINCT e.id, e.name, e.description, e.status, c.name AS course_name
                FROM experiments e
                LEFT JOIN course_experiment_assignments a ON a.experiment_id = e.id AND a.status = 'published'
                LEFT JOIN courses c ON c.id = a.course_id AND c.status = 'active'
                LEFT JOIN course_members m ON m.course_id = c.id AND m.student_id = ? AND m.status = 'active'
                WHERE (e.creator_id = ? AND e.status = 'draft')
                   OR (e.status = 'published' AND a.id IS NOT NULL AND m.student_id IS NOT NULL)
                ORDER BY e.updated_at DESC, e.id DESC
                """, (rs, rowNum) -> new DshPluginModels.ExperimentView(rs.getLong("id"), rs.getString("name"),
                rs.getString("description"), rs.getString("status"), rs.getString("course_name")), studentId, studentId);
    }

    @Transactional
    public DshPluginModels.RunView bindRun(DshPrincipal principal, long experimentId,
                                            String workspaceRef, String sessionRef) {
        if (!validOpaqueRef(workspaceRef) || !validOpaqueRef(sessionRef)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DSH_REFERENCE", "DSH 工作区或会话标识无效");
        }
        Integer instance = jdbc.query("""
                SELECT 1 FROM dsh_instances WHERE id = ? AND student_id = ? AND status = 'running'
                """, rs -> rs.next() ? 1 : null, principal.instanceId(), principal.studentId());
        if (instance == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "DSH_INSTANCE_INACTIVE", "DSH 实例未运行");
        Integer access = jdbc.query("""
                SELECT 1 FROM experiments e
                WHERE e.id = ? AND (
                  (e.creator_id = ? AND e.status = 'draft') OR
                  (e.status = 'published' AND EXISTS (
                    SELECT 1 FROM course_experiment_assignments a
                    JOIN course_members m ON m.course_id = a.course_id AND m.student_id = ? AND m.status = 'active'
                    JOIN courses c ON c.id = a.course_id AND c.status = 'active'
                    WHERE a.experiment_id = e.id AND a.status = 'published'
                  ))
                )
                """, rs -> rs.next() ? 1 : null, experimentId, principal.studentId(), principal.studentId());
        if (access == null) throw new ApiException(HttpStatus.FORBIDDEN, "EXPERIMENT_FORBIDDEN", "当前学生不能运行该实验");

        Long runId = jdbc.query("""
                SELECT id FROM experiment_runs
                WHERE experiment_id = ? AND student_id = ? AND execution_mode = 'dsh'
                """, rs -> rs.next() ? rs.getLong(1) : null, experimentId, principal.studentId());
        if (runId == null) {
            KeyHolder keys = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO experiment_runs (experiment_id, student_id, execution_mode, status, last_opened_at)
                        VALUES (?, ?, 'dsh', 'not_started', CURRENT_TIMESTAMP(6))
                        """, Statement.RETURN_GENERATED_KEYS);
                statement.setLong(1, experimentId);
                statement.setLong(2, principal.studentId());
                return statement;
            }, keys);
            runId = requiredKey(keys);
            jdbc.update("""
                    INSERT INTO node_progress (experiment_id, run_id, node_id, status, progress_percent)
                    SELECT experiment_id, ?, id,
                           CASE WHEN parent_node_id IS NULL THEN 'pending' ELSE 'locked' END, 0
                    FROM experiment_nodes WHERE experiment_id = ?
                    """, runId, experimentId);
            jdbc.update("""
                    INSERT INTO task_progress (experiment_id, run_id, task_id, status)
                    SELECT experiment_id, ?, id, 'pending' FROM node_tasks WHERE experiment_id = ?
                    """, runId, experimentId);
        }
        Long finalRunId = runId;
        jdbc.update("""
                INSERT INTO dsh_run_bindings
                  (run_id, experiment_id, student_id, instance_id, workspace_ref, session_ref)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE instance_id = VALUES(instance_id), workspace_ref = VALUES(workspace_ref),
                  session_ref = VALUES(session_ref), last_seen_at = CURRENT_TIMESTAMP(6)
                """, finalRunId, experimentId, principal.studentId(), principal.instanceId(), workspaceRef, sessionRef);
        jdbc.update("UPDATE dsh_instances SET last_active_at = CURRENT_TIMESTAMP(6) WHERE id = ?", principal.instanceId());
        return jdbc.queryForObject("SELECT id, experiment_id, status FROM experiment_runs WHERE id = ?",
                (rs, rowNum) -> new DshPluginModels.RunView(rs.getLong("id"), rs.getLong("experiment_id"),
                        rs.getString("status")), finalRunId);
    }

    public DshPluginModels.LearningPath learningPath(DshPrincipal principal, long runId) {
        Run run = requireDshRun(principal, runId);
        List<NodeRow> rows = jdbc.query("""
                SELECT n.id, n.parent_node_id, n.sequence_code, n.name, n.description,
                       np.status, CAST(np.progress_percent AS UNSIGNED) AS progress_percent
                FROM experiment_nodes n
                JOIN node_progress np ON np.node_id = n.id AND np.experiment_id = n.experiment_id AND np.run_id = ?
                WHERE n.experiment_id = ? ORDER BY n.sort_order, n.id
                """, (rs, rowNum) -> new NodeRow(rs.getLong("id"), rs.getObject("parent_node_id", Long.class),
                rs.getString("sequence_code"), rs.getString("name"), rs.getString("description"),
                rs.getString("status"), rs.getInt("progress_percent")), runId, run.experimentId());
        List<DshPluginModels.LearningNode> nodes = new ArrayList<>();
        for (NodeRow row : rows) {
            List<DshPluginModels.LearningTask> tasks = jdbc.query("""
                    SELECT nt.id, nt.title, nt.description, COALESCE(tp.status, 'pending') AS status
                    FROM node_tasks nt LEFT JOIN task_progress tp ON tp.task_id = nt.id
                      AND tp.experiment_id = nt.experiment_id AND tp.run_id = ?
                    WHERE nt.experiment_id = ? AND nt.node_id = ? ORDER BY nt.sort_order, nt.id
                    """, (rs, rowNum) -> new DshPluginModels.LearningTask(rs.getLong("id"), rs.getString("title"),
                    rs.getString("description"), rs.getString("status")), runId, run.experimentId(), row.nodeId());
            nodes.add(new DshPluginModels.LearningNode(row.nodeId(), row.parentNodeId(), row.sequenceCode(),
                    row.name(), row.description(), row.status(), row.progressPercent(), List.copyOf(tasks)));
        }
        return new DshPluginModels.LearningPath(runId, run.experimentId(), List.copyOf(nodes));
    }

    public long assertBoundRun(DshPrincipal principal, long runId) {
        return requireDshRun(principal, runId).experimentId();
    }

    @Transactional
    public void updateNode(DshPrincipal principal, long runId, long nodeId, String status, boolean confirmed) {
        if (!confirmed) throw new ApiException(HttpStatus.CONFLICT, "STUDENT_CONFIRMATION_REQUIRED", "需要学生明确确认后才能更新节点进度");
        if (!List.of("pending", "in_progress", "completed").contains(status)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PROGRESS_STATUS", "节点进度状态无效");
        }
        Run run = requireDshRun(principal, runId);
        List<String> statuses = jdbc.query("""
                SELECT status FROM node_progress WHERE run_id = ? AND experiment_id = ? AND node_id = ? FOR UPDATE
                """, (rs, rowNum) -> rs.getString(1), runId, run.experimentId(), nodeId);
        if (statuses.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "NODE_NOT_FOUND", "节点不存在或不属于当前实验");
        if ("locked".equals(statuses.get(0))) throw new ApiException(HttpStatus.CONFLICT, "NODE_LOCKED", "当前节点尚未解锁");
        if ("completed".equals(statuses.get(0)) && !"completed".equals(status)) {
            throw new ApiException(HttpStatus.CONFLICT, "NODE_ALREADY_COMPLETED", "已完成节点不能回退");
        }
        if ("completed".equals(status)) {
            Integer incomplete = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM node_tasks nt LEFT JOIN task_progress tp
                      ON tp.task_id = nt.id AND tp.run_id = ? AND tp.experiment_id = nt.experiment_id
                    WHERE nt.experiment_id = ? AND nt.node_id = ? AND COALESCE(tp.status, 'pending') <> 'completed'
                    """, Integer.class, runId, run.experimentId(), nodeId);
            if (incomplete != null && incomplete > 0) throw new ApiException(HttpStatus.CONFLICT, "NODE_TASKS_INCOMPLETE", "请先完成该节点的任务");
        }
        jdbc.update("""
                UPDATE node_progress SET status = ?, progress_percent = CASE WHEN ? = 'completed' THEN 100 ELSE progress_percent END,
                  started_at = COALESCE(started_at, CURRENT_TIMESTAMP(6)),
                  completed_at = CASE WHEN ? = 'completed' THEN CURRENT_TIMESTAMP(6) ELSE NULL END
                WHERE run_id = ? AND experiment_id = ? AND node_id = ?
                """, status, status, status, runId, run.experimentId(), nodeId);
        if ("completed".equals(status)) {
            jdbc.update("""
                    UPDATE node_progress child JOIN experiment_nodes n ON n.id = child.node_id AND n.experiment_id = child.experiment_id
                    SET child.status = 'pending' WHERE child.run_id = ? AND child.experiment_id = ?
                      AND n.parent_node_id = ? AND child.status = 'locked'
                    """, runId, run.experimentId(), nodeId);
        }
        Integer remaining = jdbc.queryForObject("""
                SELECT COUNT(*) FROM node_progress WHERE run_id = ? AND experiment_id = ? AND status <> 'completed'
                """, Integer.class, runId, run.experimentId());
        boolean allCompleted = remaining != null && remaining == 0;
        jdbc.update("""
                UPDATE experiment_runs SET status = ?, current_node_id = ?,
                  started_at = COALESCE(started_at, CURRENT_TIMESTAMP(6)),
                  completed_at = CASE WHEN ? THEN COALESCE(completed_at, CURRENT_TIMESTAMP(6)) ELSE NULL END,
                  last_opened_at = CURRENT_TIMESTAMP(6)
                WHERE id = ? AND execution_mode = 'dsh'
                """, allCompleted ? "completed" : "in_progress", allCompleted ? null : nodeId, allCompleted, runId);
    }

    @Transactional
    public void updateTask(DshPrincipal principal, long runId, long taskId, String status, boolean confirmed) {
        if (!confirmed) throw new ApiException(HttpStatus.CONFLICT, "STUDENT_CONFIRMATION_REQUIRED", "需要学生明确确认后才能更新任务进度");
        if (!List.of("pending", "in_progress", "completed", "failed").contains(status)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PROGRESS_STATUS", "任务进度状态无效");
        }
        Run run = requireDshRun(principal, runId);
        Integer updated = jdbc.query("""
                SELECT np.status FROM node_tasks nt JOIN node_progress np
                  ON np.node_id = nt.node_id AND np.experiment_id = nt.experiment_id AND np.run_id = ?
                WHERE nt.id = ? AND nt.experiment_id = ? FOR UPDATE
                """, rs -> rs.next() && !"locked".equals(rs.getString(1)) && !"completed".equals(rs.getString(1)) ? 1 : null,
                runId, taskId, run.experimentId());
        if (updated == null) throw new ApiException(HttpStatus.CONFLICT, "TASK_UNAVAILABLE", "任务不存在、尚未解锁或节点已完成");
        jdbc.update("""
                UPDATE task_progress SET status = ?, completed_at = CASE WHEN ? = 'completed' THEN CURRENT_TIMESTAMP(6) ELSE NULL END
                WHERE run_id = ? AND experiment_id = ? AND task_id = ?
                """, status, status, runId, run.experimentId(), taskId);
    }

    @Transactional
    public DshPluginModels.CodeSubmissionView submitCode(DshPrincipal principal, long runId, Long nodeId,
                                                          String description, List<CleanFile> files) {
        Run run = requireDshRun(principal, runId);
        if (nodeId != null) {
            Integer nodeExists = jdbc.query("SELECT 1 FROM experiment_nodes WHERE id = ? AND experiment_id = ?",
                    rs -> rs.next() ? 1 : null, nodeId, run.experimentId());
            if (nodeExists == null) throw new ApiException(HttpStatus.BAD_REQUEST, "NODE_NOT_IN_EXPERIMENT", "节点不属于当前实验");
        }
        long total = files.stream().mapToLong(file -> file.sizeBytes()).sum();
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO code_submissions (run_id, experiment_id, student_id, node_id, description, file_count, total_bytes)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, runId);
            statement.setLong(2, run.experimentId());
            statement.setLong(3, principal.studentId());
            if (nodeId == null) statement.setObject(4, null); else statement.setLong(4, nodeId);
            statement.setString(5, description);
            statement.setInt(6, files.size());
            statement.setLong(7, total);
            return statement;
        }, keys);
        long submissionId = requiredKey(keys);
        for (CleanFile file : files) {
            jdbc.update("""
                    INSERT INTO code_submission_files (submission_id, relative_path, content_text, content_sha256, size_bytes)
                    VALUES (?, ?, ?, ?, ?)
                    """, submissionId, file.path(), file.content(), file.sha256(), file.sizeBytes());
        }
        Timestamp createdAt = jdbc.queryForObject("SELECT created_at FROM code_submissions WHERE id = ?",
                Timestamp.class, submissionId);
        return new DshPluginModels.CodeSubmissionView(submissionId, runId, run.experimentId(), nodeId,
                files.size(), total, createdAt.toInstant());
    }

    public List<DshPluginModels.ExperimentView> teacherExperiments(long teacherId) {
        return jdbc.query("""
                SELECT DISTINCT e.id, e.name, e.description, e.status, c.name AS course_name
                FROM experiments e
                JOIN course_experiment_assignments a ON a.experiment_id = e.id AND a.status = 'published'
                JOIN courses c ON c.id = a.course_id AND c.teacher_id = ? AND c.status = 'active'
                UNION
                SELECT e.id, e.name, e.description, e.status, NULL AS course_name
                FROM experiments e JOIN experiment_reviewers er ON er.experiment_id = e.id AND er.teacher_id = ?
                WHERE NOT EXISTS (SELECT 1 FROM course_experiment_assignments a
                                  WHERE a.experiment_id = e.id AND a.status = 'published')
                ORDER BY name, id
                """, (rs, rowNum) -> new DshPluginModels.ExperimentView(rs.getLong("id"), rs.getString("name"),
                rs.getString("description"), rs.getString("status"), rs.getString("course_name")), teacherId, teacherId);
    }

    public List<DshPluginModels.CodeSubmissionView> submissionsForTeacher(long teacherId, long runId) {
        Run run = requireTeacherRun(teacherId, runId);
        return jdbc.query("""
                SELECT id, run_id, experiment_id, node_id, file_count, total_bytes, created_at
                FROM code_submissions WHERE run_id = ? ORDER BY created_at DESC, id DESC
                """, (rs, rowNum) -> new DshPluginModels.CodeSubmissionView(rs.getLong("id"), rs.getLong("run_id"),
                rs.getLong("experiment_id"), rs.getObject("node_id", Long.class), rs.getInt("file_count"),
                rs.getLong("total_bytes"), rs.getTimestamp("created_at").toInstant()), runId);
    }

    public List<DshPluginModels.TeacherRunView> runsForTeacherExperiment(long teacherId, long experimentId) {
        assertTeacherExperiment(teacherId, experimentId);
        return jdbc.query("""
                SELECT r.id AS run_id, r.experiment_id, u.id AS student_id, u.display_name, u.email,
                       r.status, COALESCE(SUM(CASE WHEN np.status = 'completed' THEN 1 ELSE 0 END), 0) AS completed_nodes,
                       COUNT(np.id) AS total_nodes,
                       COALESCE((SELECT COUNT(*) FROM task_progress tp WHERE tp.run_id = r.id AND tp.status = 'completed'), 0) AS completed_tasks,
                       COALESCE((SELECT COUNT(*) FROM task_progress tp WHERE tp.run_id = r.id), 0) AS total_tasks,
                       r.last_opened_at
                FROM experiment_runs r JOIN users u ON u.id = r.student_id
                LEFT JOIN node_progress np ON np.run_id = r.id AND np.experiment_id = r.experiment_id
                WHERE r.experiment_id = ? AND r.execution_mode = 'dsh' AND (
                  EXISTS (SELECT 1 FROM course_experiment_assignments a JOIN courses c ON c.id = a.course_id
                          JOIN course_members m ON m.course_id = c.id AND m.student_id = r.student_id AND m.status = 'active'
                          WHERE a.experiment_id = r.experiment_id AND a.status = 'published'
                            AND c.teacher_id = ? AND c.status = 'active') OR
                  (NOT EXISTS (SELECT 1 FROM course_experiment_assignments a
                               WHERE a.experiment_id = r.experiment_id AND a.status = 'published') AND
                   EXISTS (SELECT 1 FROM experiment_reviewers er
                           WHERE er.experiment_id = r.experiment_id AND er.teacher_id = ?))
                )
                GROUP BY r.id, r.experiment_id, u.id, u.display_name, u.email, r.status, r.last_opened_at
                ORDER BY r.last_opened_at DESC, r.id DESC
                """, (rs, rowNum) -> new DshPluginModels.TeacherRunView(rs.getLong("run_id"),
                rs.getLong("experiment_id"), rs.getLong("student_id"), rs.getString("display_name"),
                rs.getString("email"), rs.getString("status"), rs.getInt("completed_nodes"),
                rs.getInt("total_nodes"), rs.getInt("completed_tasks"), rs.getInt("total_tasks"),
                rs.getTimestamp("last_opened_at") == null ? null : rs.getTimestamp("last_opened_at").toInstant()),
                experimentId, teacherId, teacherId);
    }

    public DshPluginModels.SubmissionDetail submissionForTeacher(long teacherId, long runId, long submissionId) {
        requireTeacherRun(teacherId, runId);
        List<DshPluginModels.CodeSubmissionView> submissions = jdbc.query("""
                SELECT id, run_id, experiment_id, node_id, file_count, total_bytes, created_at
                FROM code_submissions WHERE id = ? AND run_id = ?
                """, (rs, rowNum) -> new DshPluginModels.CodeSubmissionView(rs.getLong("id"), rs.getLong("run_id"),
                rs.getLong("experiment_id"), rs.getObject("node_id", Long.class), rs.getInt("file_count"),
                rs.getLong("total_bytes"), rs.getTimestamp("created_at").toInstant()), submissionId, runId);
        if (submissions.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "SUBMISSION_NOT_FOUND", "代码提交不存在");
        List<DshPluginModels.FileView> files = jdbc.query("""
                SELECT relative_path, content_sha256, size_bytes, content_text
                FROM code_submission_files WHERE submission_id = ? ORDER BY relative_path
                """, (rs, rowNum) -> new DshPluginModels.FileView(rs.getString("relative_path"),
                rs.getString("content_sha256"), rs.getLong("size_bytes"), rs.getString("content_text")), submissionId);
        return new DshPluginModels.SubmissionDetail(submissions.get(0), List.copyOf(files));
    }

    @Transactional
    public TeacherModels.ReviewView reviewDshRun(long teacherId, long runId, TeacherModels.ReviewRequest request) {
        Run run = requireTeacherRun(teacherId, runId);
        Long courseId = jdbc.query("""
                SELECT a.course_id FROM course_experiment_assignments a
                JOIN courses c ON c.id = a.course_id AND c.teacher_id = ? AND c.status = 'active'
                JOIN course_members m ON m.course_id = c.id AND m.student_id = ? AND m.status = 'active'
                WHERE a.experiment_id = ? AND a.status = 'published' LIMIT 1
                """, rs -> rs.next() ? rs.getLong(1) : null, teacherId, run.studentId(), run.experimentId());
        Long finalCourseId = courseId;
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO teacher_reviews (course_id, experiment_id, run_id, teacher_id, student_id, rating, feedback)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE course_id = VALUES(course_id), rating = VALUES(rating),
                      feedback = VALUES(feedback), updated_at = CURRENT_TIMESTAMP(6)
                    """);
            if (finalCourseId == null) statement.setObject(1, null); else statement.setLong(1, finalCourseId);
            statement.setLong(2, run.experimentId());
            statement.setLong(3, runId);
            statement.setLong(4, teacherId);
            statement.setLong(5, run.studentId());
            if (request.rating() == null) statement.setObject(6, null); else statement.setInt(6, request.rating());
            statement.setString(7, request.feedback());
            return statement;
        });
        return findDshReview(teacherId, runId);
    }

    public TeacherModels.ReviewView findDshReview(long teacherId, long runId) {
        requireTeacherRun(teacherId, runId);
        List<TeacherModels.ReviewView> rows = jdbc.query("""
                SELECT id, rating, feedback, updated_at FROM teacher_reviews
                WHERE run_id = ? AND teacher_id = ?
                """, (rs, rowNum) -> new TeacherModels.ReviewView(rs.getLong("id"),
                rs.getObject("rating", Integer.class), rs.getString("feedback"),
                rs.getTimestamp("updated_at").toInstant()), runId, teacherId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Run requireDshRun(DshPrincipal principal, long runId) {
        List<Run> rows = jdbc.query("""
                SELECT r.id, r.experiment_id, r.student_id FROM experiment_runs r
                JOIN dsh_run_bindings b ON b.run_id = r.id AND b.experiment_id = r.experiment_id
                JOIN dsh_instances i ON i.id = b.instance_id AND i.student_id = b.student_id AND i.status = 'running'
                WHERE r.id = ? AND r.student_id = ? AND r.execution_mode = 'dsh' AND b.instance_id = ?
                """, (rs, rowNum) -> new Run(rs.getLong("id"), rs.getLong("experiment_id"), rs.getLong("student_id")),
                runId, principal.studentId(), principal.instanceId());
        if (rows.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "RUN_NOT_FOUND", "DSH 实验运行不存在或无权访问");
        return rows.get(0);
    }

    private Run requireTeacherRun(long teacherId, long runId) {
        List<Run> rows = jdbc.query("""
                SELECT r.id, r.experiment_id, r.student_id FROM experiment_runs r
                WHERE r.id = ? AND r.execution_mode = 'dsh' AND (
                  EXISTS (SELECT 1 FROM course_experiment_assignments a JOIN courses c ON c.id = a.course_id
                          WHERE a.experiment_id = r.experiment_id AND a.status = 'published'
                            AND c.teacher_id = ? AND c.status = 'active') OR
                  (NOT EXISTS (SELECT 1 FROM course_experiment_assignments a
                               WHERE a.experiment_id = r.experiment_id AND a.status = 'published') AND
                   EXISTS (SELECT 1 FROM experiment_reviewers er
                          WHERE er.experiment_id = r.experiment_id AND er.teacher_id = ?)
                  )
                )
                """, (rs, rowNum) -> new Run(rs.getLong("id"), rs.getLong("experiment_id"), rs.getLong("student_id")), runId, teacherId, teacherId);
        if (rows.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "RUN_NOT_FOUND", "DSH 实验运行不存在或无权访问");
        return rows.get(0);
    }

    private void assertTeacherExperiment(long teacherId, long experimentId) {
        Integer allowed = jdbc.query("""
                SELECT 1 WHERE EXISTS (
                  SELECT 1 FROM course_experiment_assignments a JOIN courses c ON c.id = a.course_id
                  WHERE a.experiment_id = ? AND a.status = 'published' AND c.teacher_id = ? AND c.status = 'active'
                ) OR (
                  NOT EXISTS (SELECT 1 FROM course_experiment_assignments a
                              WHERE a.experiment_id = ? AND a.status = 'published') AND
                  EXISTS (SELECT 1 FROM experiment_reviewers er
                          WHERE er.experiment_id = ? AND er.teacher_id = ?)
                )
                """, rs -> rs.next() ? 1 : null, experimentId, teacherId, experimentId, experimentId, teacherId);
        if (allowed == null) throw new ApiException(HttpStatus.NOT_FOUND, "EXPERIMENT_FORBIDDEN", "无权查看该实验的 DSH 学习记录");
    }

    private boolean validOpaqueRef(String value) {
        return value != null && value.matches("[A-Za-z0-9_-]{1,200}");
    }

    private long requiredKey(KeyHolder keys) {
        Number key = keys.getKey();
        if (key == null) throw new IllegalStateException("数据库未返回 DSH 记录主键");
        return key.longValue();
    }

    public record CleanFile(String path, String content, String sha256, long sizeBytes) {}
    private record Run(long id, long experimentId, long studentId) {}
    private record NodeRow(long nodeId, Long parentNodeId, String sequenceCode, String name, String description,
                           String status, int progressPercent) {}
}
