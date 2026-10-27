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
    public ExperimentReviewerModels.AssignmentView assign(long experimentId, long teacherId, long courseId) {
        Integer personalExperiment = jdbc.query("""
                SELECT 1 FROM experiments e
                WHERE e.id = ? AND EXISTS (
                  SELECT 1 FROM course_members m JOIN courses c ON c.id = m.course_id
                  WHERE m.student_id = e.creator_id AND m.status = 'active'
                    AND c.id = ? AND c.teacher_id = ? AND c.status = 'active'
                ) AND NOT EXISTS (
                  SELECT 1 FROM course_experiment_assignments a
                  WHERE a.experiment_id = e.id AND a.status = 'published'
                )
                """, rs -> rs.next() ? 1 : null, experimentId, courseId, teacherId);
        if (personalExperiment == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "EXPERIMENT_REVIEW_FORBIDDEN", "只能管理自己课程学生的个人实验");
        }
        jdbc.update("""
                INSERT INTO experiment_reviewers (experiment_id, course_id, teacher_id, assigned_by_teacher_id)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE experiment_id = VALUES(experiment_id)
                """, experimentId, courseId, teacherId, teacherId);
        int claimed = jdbc.update("""
                UPDATE experiment_reviewers
                SET course_id = ?, teacher_id = ?, assigned_by_teacher_id = ?
                WHERE experiment_id = ? AND (course_id IS NULL OR (course_id = ? AND teacher_id = ?))
                """, courseId, teacherId, teacherId, experimentId, courseId, teacherId);
        if (claimed == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "EXPERIMENT_REVIEW_ALREADY_ASSIGNED",
                    "该个人实验已绑定其他课程教师，不能跨课程更改审核关系");
        }
        return find(experimentId, courseId, teacherId);
    }

    public ExperimentReviewerModels.AssignmentView find(long experimentId, long currentTeacherId) {
        List<Long> courseIds = jdbc.query("""
                SELECT MIN(c.id) FROM course_members m JOIN courses c ON c.id = m.course_id
                JOIN experiments e ON e.creator_id = m.student_id
                WHERE e.id = ? AND m.status = 'active' AND c.status = 'active' AND c.teacher_id = ?
                """, (rs, rowNum) -> { long id = rs.getLong(1); return rs.wasNull() ? null : id; },
                experimentId, currentTeacherId);
        if (courseIds.isEmpty() || courseIds.get(0) == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "EXPERIMENT_REVIEW_FORBIDDEN", "无权管理该个人实验");
        }
        return find(experimentId, courseIds.get(0), currentTeacherId);
    }

    private ExperimentReviewerModels.AssignmentView find(long experimentId, long courseId, long currentTeacherId) {
        List<ExperimentReviewerModels.AssignmentView> rows = jdbc.query("""
                SELECT er.experiment_id, er.course_id, er.teacher_id, u.display_name, er.assigned_by_teacher_id,
                       er.created_at, er.updated_at
                FROM experiment_reviewers er JOIN users u ON u.id = er.teacher_id
                JOIN courses c ON c.id = er.course_id AND c.status = 'active'
                JOIN course_members m ON m.course_id = c.id AND m.status = 'active'
                JOIN experiments e ON e.id = er.experiment_id AND e.creator_id = m.student_id
                WHERE er.experiment_id = ? AND er.course_id = ? AND er.teacher_id = ? AND c.teacher_id = ?
                """, (rs, rowNum) -> new ExperimentReviewerModels.AssignmentView(
                rs.getLong("experiment_id"), rs.getLong("course_id"), rs.getLong("teacher_id"), rs.getString("display_name"),
                rs.getLong("assigned_by_teacher_id"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()), experimentId, courseId, currentTeacherId, currentTeacherId);
        if (rows.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "EXPERIMENT_REVIEWER_NOT_FOUND", "该个人实验尚未绑定当前课程教师");
        return rows.get(0);
    }

    public List<ExperimentReviewerModels.ReviewableExperimentView> reviewableExperiments(long teacherId) {
        return jdbc.query("""
                SELECT e.id, eligible.course_id, c.name AS course_name, e.name, e.status,
                       er.teacher_id, u.display_name AS teacher_name
                FROM experiments e
                JOIN (
                  SELECT e2.id AS experiment_id, MIN(c2.id) AS course_id
                  FROM experiments e2 JOIN course_members m2 ON m2.student_id = e2.creator_id AND m2.status = 'active'
                  JOIN courses c2 ON c2.id = m2.course_id AND c2.status = 'active' AND c2.teacher_id = ?
                  GROUP BY e2.id
                ) eligible ON eligible.experiment_id = e.id
                JOIN courses c ON c.id = eligible.course_id
                LEFT JOIN experiment_reviewers er ON er.experiment_id = e.id
                LEFT JOIN users u ON u.id = er.teacher_id AND u.status = 'active'
                WHERE (er.experiment_id IS NULL OR er.course_id IS NULL
                       OR (er.course_id = eligible.course_id AND er.teacher_id = ?))
                  AND e.status <> 'archived'
                  AND NOT EXISTS (SELECT 1 FROM course_experiment_assignments a
                                  WHERE a.experiment_id = e.id AND a.status = 'published')
                ORDER BY e.updated_at DESC, e.id DESC
                """, (rs, rowNum) -> new ExperimentReviewerModels.ReviewableExperimentView(
                rs.getLong("id"), rs.getLong("course_id"), rs.getString("course_name"),
                rs.getString("name"), rs.getString("status"),
                rs.getObject("teacher_id", Long.class), rs.getString("teacher_name")), teacherId, teacherId);
    }
}
