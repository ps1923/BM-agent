package com.bmhs.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class CourseModels {
    private CourseModels() {}

    public record CreateCourseRequest(@NotBlank @Size(max = 150) String name) {}

    public record JoinCourseRequest(@NotBlank @Size(max = 16) String inviteCode) {}

    public record PublishExperimentRequest(@NotNull Long courseId) {}

    public record PublishedExperiment(long experimentId, String name, String description,
                                      Instant publishedAt) {}

    public record CourseView(long id, long teacherId, String name, String inviteCode,
                             int memberCount, List<PublishedExperiment> experiments) {}

    public record CourseMember(long userId, String email, String displayName, Instant joinedAt) {}

    public record ExperimentSummary(long id, String name, String description, String status) {}
}
