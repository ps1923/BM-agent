package com.bmhs.patch;

import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class PatchModels {
    private PatchModels() {}

    public record PatchFile(String path, String expectedHash, String content) {}

    public record PatchPayload(List<PatchFile> files) {}

    public record PatchView(long id, long runId, long workspaceId, String description,
                            String status, List<PatchFile> files, Instant createdAt, Instant appliedAt,
                            Long snapshotId) {}

    public record PatchApplyView(long patchId, String status, long snapshotId,
                                 List<String> appliedFiles) {}

    public record PatchCreateRequest(@Size(max = 500) String description,
                                     @Size(max = 20) List<PatchFile> files) {}
}
