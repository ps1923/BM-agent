package com.bmhs.teacher;

import jakarta.validation.constraints.Positive;

import java.time.Instant;

public final class ExperimentReviewerModels {
    private ExperimentReviewerModels() {}

    public record AssignmentRequest(@Positive long teacherId) {}

    public record AssignmentView(long experimentId, long teacherId, String teacherName,
                                 long assignedByAdminId, Instant assignedAt, Instant updatedAt) {}

    public record ReviewableExperimentView(long experimentId, String name, String status,
                                            Long teacherId, String teacherName) {}

    public record TeacherOption(long teacherId, String displayName, String email) {}
}
