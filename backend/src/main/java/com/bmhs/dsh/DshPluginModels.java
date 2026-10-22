package com.bmhs.dsh;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class DshPluginModels {
    private DshPluginModels() {}

    public record RunBindRequest(@Positive long experimentId,
                                 @NotBlank @Size(max = 200) String workspaceRef,
                                 @NotBlank @Size(max = 200) String sessionRef) {}

    public record RunView(long runId, long experimentId, String status) {}

    public record ExperimentView(long experimentId, String name, String description, String status,
                                 String courseName) {}

    public record LearningPath(long runId, long experimentId, List<LearningNode> nodes) {}

    public record LearningNode(long nodeId, Long parentNodeId, String sequenceCode, String name,
                               String description, String status, int progressPercent, List<LearningTask> tasks) {}

    public record LearningTask(long taskId, String title, String description, String status) {}

    public record ProgressUpdateRequest(@NotBlank @Size(max = 20) String status,
                                        @NotNull Boolean studentConfirmed) {}

    public record CodeSubmissionRequest(Long nodeId, @Size(max = 500) String description,
                                        @NotEmpty @Size(max = 50) List<@Valid SubmittedFile> files) {}

    public record SubmittedFile(@NotBlank @Size(max = 700) String path,
                                @NotNull @Size(max = 2_000_000) String content,
                                @NotBlank @Size(min = 64, max = 64) String sha256) {}

    public record CodeSubmissionView(long submissionId, long runId, long experimentId, Long nodeId,
                                     int fileCount, long totalBytes, Instant createdAt) {}

    public record FileView(String path, String sha256, long sizeBytes, String content) {}

    public record SubmissionDetail(CodeSubmissionView submission, List<FileView> files) {}

    public record TeacherRunView(long runId, long experimentId, long studentId, String studentName,
                                 String studentEmail, String status, int completedNodes, int totalNodes,
                                 int completedTasks, int totalTasks, Instant lastOpenedAt, String executionMode) {
        public TeacherRunView(long runId, long experimentId, long studentId, String studentName,
                              String studentEmail, String status, int completedNodes, int totalNodes,
                              int completedTasks, int totalTasks, Instant lastOpenedAt) {
            this(runId, experimentId, studentId, studentName, studentEmail, status, completedNodes,
                    totalNodes, completedTasks, totalTasks, lastOpenedAt, "dsh");
        }
    }
}
