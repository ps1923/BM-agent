package com.bmhs.teacher;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class TeacherModels {
    private TeacherModels() {}

    public record StudentProgress(long studentId, String displayName, String email, long runId,
                                  long experimentId, String experimentName, String runStatus,
                                  int completedNodes, int totalNodes, int completedTasks, int totalTasks,
                                  Instant lastOpenedAt) {}

    public record CourseDashboard(long courseId, String courseName, int memberCount,
                                  int experimentCount, List<StudentProgress> students) {}

    public record TimelineEvent(String source, String title, String detail, Instant createdAt) {}

    public record RunSummary(long runId, long experimentId, String experimentName, String runStatus,
                             int completedNodes, int totalNodes, int completedTasks, int totalTasks,
                             int commandCount, int messageCount,
                             int bugCount, boolean aiSummaryAvailable, String aiSummary,
                             ReviewView review) {}

    public record ReviewRequest(@Min(0) @Max(100) Integer rating, @Size(max = 2000) String feedback) {}

    public record ReviewView(long id, Integer rating, String feedback, Instant updatedAt) {}

    public record StageEvidence(long nodeId, String nodeName, String stageName, String status,
                               Instant completedAt, Long snapshotId, Instant snapshotCreatedAt) {}

    public record SnapshotFile(String path, long sizeBytes, boolean text) {}

    public record SnapshotFileContent(SnapshotFile file, String content) {}
}
