package com.bmhs.rag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class RagModels {
    private RagModels() {}

    public record BugRequest(@NotBlank @Size(max = 200) String title,
                             @NotBlank @Size(max = 20000) String problem,
                             @Size(max = 20000) String solution,
                             @Size(max = 100) String technologyStack,
                             Long nodeId) {
        public BugRequest(String title, String problem, String solution, Long nodeId) {
            this(title, problem, solution, null, nodeId);
        }
    }

    public record BugReviewRequest(@NotBlank @jakarta.validation.constraints.Pattern(regexp = "approved|rejected") String decision,
                                   @Size(max = 1000) String feedback) {}

    public record BugCaseView(long id, long experimentId, long runId, Long nodeId, Long courseId,
                              long studentId, String title, String problem, String solution,
                              String technologyStack, String status, String visibility, String vectorStatus,
                              String chromaDocumentId, Instant createdAt, Instant updatedAt) {
        public BugCaseView(long id, long experimentId, long runId, Long nodeId, Long courseId,
                           long studentId, String title, String problem, String solution,
                           String status, String visibility, String vectorStatus,
                           String chromaDocumentId, Instant createdAt, Instant updatedAt) {
            this(id, experimentId, runId, nodeId, courseId, studentId, title, problem, solution,
                    null, status, visibility, vectorStatus, chromaDocumentId, createdAt, updatedAt);
        }
    }

    public record RagReference(long bugCaseId, String title, String problem, String solution,
                               double distance) {}

    public record RagSearchResult(boolean available, List<RagReference> references) {}

    public record ChromaHit(String id, double distance) {}

    public record IndexRequest(String documentId, String document, Long courseId,
                               long experimentId, Long nodeId, String technologyStack, String status) {}
}
