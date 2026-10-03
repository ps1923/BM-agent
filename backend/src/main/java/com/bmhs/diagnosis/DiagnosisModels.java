package com.bmhs.diagnosis;

import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class DiagnosisModels {
    private DiagnosisModels() {}

    public record DiagnosisRequest(@Size(max = 2000) String question) {}
    public record Finding(String severity, String title, String detail,
                          List<String> files, List<String> verificationSteps) {}
    public record PatchSummary(long id, String description, List<String> files) {}
    public record DiagnosisView(long id, long runId, String status, String summary,
                                List<Finding> findings, List<PatchSummary> patches,
                                Instant createdAt, Instant startedAt, Instant completedAt,
                                String error) {}

    public record Result(String summary, List<Finding> findings, List<PatchSummary> patches) {}
}
