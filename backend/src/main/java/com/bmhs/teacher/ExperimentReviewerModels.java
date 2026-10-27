package com.bmhs.teacher;

import jakarta.validation.constraints.Positive;

import java.time.Instant;

public final class ExperimentReviewerModels {
    private ExperimentReviewerModels() {}

    public record AssignmentRequest(@Positive long teacherId, @Positive long courseId) {}

    public record AssignmentView(long experimentId, long courseId, long teacherId, String teacherName,
                                 long assignedByTeacherId, Instant assignedAt, Instant updatedAt) {}

    public record ReviewableExperimentView(long experimentId, long courseId, String courseName,
                                            String name, String status,
                                            Long teacherId, String teacherName) {}

}
