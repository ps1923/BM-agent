package com.bmhs.teacher;

import com.bmhs.experimentcreation.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class ExperimentReviewerRepository {
    private final JdbcTemplate jdbc;

    public ExperimentReviewerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public ExperimentReviewerModels.AssignmentView assign(long experimentId, long teacherId, long adminId) {
        Integer personalExperiment = jdbc.query("""
                SELECT 1 FROM experiments e
                WHERE e.id = ? AND NOT EXISTS (
                  SELECT 1 FROM course_experiment_assignments a
                  WHERE a.experiment_id = e.id AND a.status = 'published'
                )
                """, rs -> rs.next() ? 1 : null, experimentId);
        if (personalExperiment == null) {
            throw new ApiException(HttpStatus.CONFLICT, "COURSE_EXPERIMENT_REVIEWER_MANAGED", "课程实验由所属课程教师审核");
        }
        Integer teacher = jdbc.query("SELECT 1 FROM users WHERE id = ? AND role = 'teacher' AND status = 'active'",
                rs -> rs.next() ? 1 : null, teacherId);
        if (teacher == null) throw new ApiException(HttpStatus.BAD_REQUEST, "TEACHER_NOT_FOUND", "指定教师不存在或不可用");
        jdbc.update("""
                INSERT INTO experiment_reviewers (experiment_id, teacher_id, assigned_by_admin_id)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE teacher_id = VALUES(teacher_id), assigned_by_admin_id = VALUES(assigned_by_admin_id)
                """, experimentId, teacherId, adminId);
        return find(experimentId);
    }

    public ExperimentReviewerModels.AssignmentView find(long experimentId) {
        List<ExperimentReviewerModels.AssignmentView> rows = jdbc.query("""
                SELECT er.experiment_id, er.teacher_id, u.display_name, er.assigned_by_admin_id,
                       er.created_at, er.updated_at
                FROM experiment_reviewers er JOIN users u ON u.id = er.teacher_id
                WHERE er.experiment_id = ?
                """, (rs, rowNum) -> new ExperimentReviewerModels.AssignmentView(
                rs.getLong("experiment_id"), rs.getLong("teacher_id"), rs.getString("display_name"),
                rs.getLong("assigned_by_admin_id"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()), experimentId);
        if (rows.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "EXPERIMENT_REVIEWER_NOT_FOUND", "该个人实验尚未绑定审核教师");
        return rows.get(0);
    }

    public List<ExperimentReviewerModels.ReviewableExperimentView> reviewableExperiments() {
        return jdbc.query("""
                SELECT e.id, e.name, e.status, er.teacher_id, u.display_name AS teacher_name
                FROM experiments e
                LEFT JOIN experiment_reviewers er ON er.experiment_id = e.id
                LEFT JOIN users u ON u.id = er.teacher_id AND u.status = 'active'
                WHERE NOT EXISTS (SELECT 1 FROM course_experiment_assignments a
                                  WHERE a.experiment_id = e.id AND a.status = 'published')
                ORDER BY e.updated_at DESC, e.id DESC
                """, (rs, rowNum) -> new ExperimentReviewerModels.ReviewableExperimentView(
                rs.getLong("id"), rs.getString("name"), rs.getString("status"),
                rs.getObject("teacher_id", Long.class), rs.getString("teacher_name")));
    }

    public List<ExperimentReviewerModels.TeacherOption> activeTeachers() {
        return jdbc.query("""
                SELECT id, display_name, email FROM users
                WHERE role = 'teacher' AND status = 'active'
                ORDER BY display_name, id
                """, (rs, rowNum) -> new ExperimentReviewerModels.TeacherOption(
                rs.getLong("id"), rs.getString("display_name"), rs.getString("email")));
    }
}
