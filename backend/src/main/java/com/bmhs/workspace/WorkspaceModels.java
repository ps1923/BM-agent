package com.bmhs.workspace;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class WorkspaceModels {
    private WorkspaceModels() {}

    public record RunView(long runId, long experimentId, String runStatus, Long currentNodeId,
                          long workspaceId, String workspaceStatus) {}

    public record NodeProgressView(long nodeId, String status, BigDecimal progressPercent,
                                   Instant startedAt, Instant completedAt) {}

    public record TaskProgressView(long taskId, long nodeId, String status, Instant completedAt) {}

    public record RunProgressView(long runId, long experimentId, String runStatus, Long currentNodeId,
                                  List<NodeProgressView> nodes, List<TaskProgressView> tasks) {}

    public record UpdateNodeRequest(@NotNull @Pattern(regexp = "in_progress|completed") String status,
                                    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal progressPercent) {}

    public record UpdateTaskRequest(@NotNull @Pattern(regexp = "pending|completed") String status) {}

    public record SnapshotRequest(@Size(max = 500) String description) {}

    public record SnapshotView(long id, long workspaceId, Long nodeId, String storageKey, String snapshotType,
                               String description, Instant createdAt) {
        public SnapshotView(long id, long workspaceId, String storageKey, String snapshotType,
                            String description, Instant createdAt) {
            this(id, workspaceId, null, storageKey, snapshotType, description, createdAt);
        }
    }

    public record WorkspaceFile(long workspaceId, String path, String displayName, String language,
                                long sizeBytes, String contentHash, boolean entry, Instant updatedAt) {}

    public record WorkspaceFileContent(WorkspaceFile file, String content) {}

    public record WriteFileRequest(@NotNull @Size(max = 2_000_000) String content,
                                   String expectedHash) {}

    public record WorkspaceConfig(String rootPath, String runtimeImage, BigDecimal cpuLimit,
                                  int memoryLimitMb, int maxFileBytes) {}

    public record WorkspaceRecord(long id, long runId, long experimentId, long studentId,
                                 String storagePath, String runtimeImage, String runtimeInstanceId, String status,
                                 BigDecimal cpuLimit, int memoryLimitMb) {}

    public record RunRecord(long id, long experimentId, long studentId, String status,
                            Long currentNodeId, WorkspaceRecord workspace) {}
}
