package com.bmhs.git;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import com.bmhs.experimentcreation.CreationModels.AcceptanceCriterion;

public final class GitModels {
    private GitModels() {}

    public record RunView(long runId, long experimentId, String executionMode, String status) {}

    public record RepositoryView(String repoUrl, String cloneUrl, String branch, String setupInstructions) {}

    public record CredentialsView(String username, String password, String authMethod) {}

    public record SubmitRequest(
            @NotBlank @Pattern(regexp = "(?i)[0-9a-f]{40,64}") String commitSha,
            @NotEmpty @Size(max = 100) List<Long> nodeIds,
            @Size(max = 500) String description) {}

    public record SubmitResponse(long submissionId, String status) {}

    public record StudentSubmissionView(long submissionId, String commitSha, String status,
                                        List<NodeResult> results, Instant createdAt) {}

    public record StudentSubmissionSummary(long submissionId, String commitSha, String status,
                                           String description, List<Long> nodeIds, List<NodeResult> results, Instant createdAt) {}

    public record Evidence(String path, Integer lineStart, Integer lineEnd, String excerpt) {}

    public record BugDraft(String title, String problem, String solution, String technologyStack) {}

    public record CriterionResult(int criterionIndex, String decision, String summary, List<Evidence> evidence) {}

    public record NodeResult(long nodeId, String decision, String summary, List<Evidence> evidence,
                             List<String> suggestions, BugDraft bugDraft, List<CriterionResult> criterionResults,
                             Integer criteriaVersion, String criteriaSource, List<AcceptanceCriterion> criteriaSnapshot) {
        public NodeResult(long nodeId, String decision, String summary, List<Evidence> evidence,
                          List<String> suggestions, BugDraft bugDraft) {
            this(nodeId, decision, summary, evidence, suggestions, bugDraft, List.of(), null,
                    "legacy_inferred", List.of());
        }
    }

    public record BugDraftConfirmRequest(@NotBlank @Size(max = 200) String title,
                                         @NotBlank @Size(max = 20000) String problem,
                                         @Size(max = 20000) String solution,
                                         @Size(max = 100) String technologyStack) {}

    public record TeacherSubmissionSummary(long submissionId, String commitSha, String status,
                                           List<Long> nodeIds, String commitMessage,
                                           Instant committedAt, Instant createdAt) {}

    public record CommitMetadata(String commitSha, String message, String author,
                                 Instant committedAt, List<String> parentShas) {}

    public record DiffFile(String path, String status, int additions, int deletions, String patch) {}

    public record TeacherSubmissionDetail(long submissionId, long runId, long experimentId,
                                          long studentId, String commitSha, String status,
                                          String description, CommitMetadata commit,
                                          List<DiffFile> diff, List<NodeResult> results,
                                          Instant createdAt) {}

    public record WebhookAck(boolean accepted, boolean matchedSubmission) {}

    public record CommitFile(String path, String blobSha, String content) {}

    public record CommitInfo(String sha, String treeSha, String message, String author, Instant committedAt,
                             List<String> parentShas) {}

    public record RegisteredRun(long runId, long experimentId, long studentId, String status) {}

    public record RepositoryRecord(long runId, long experimentId, long studentId, Long giteaRepoId,
                                   String owner, String name, String repoUrl, String cloneUrl,
                                   String branch, String credentialUsername, String credentialCiphertext,
                                   String status) {}

    public record GiteaRepository(long id, String owner, String name, String bootstrapPassword) {
        public GiteaRepository(long id, String owner, String name) {
            this(id, owner, name, null);
        }
    }

    public record SubmissionRecord(long id, long runId, long experimentId, long studentId, String commitSha,
                                   String parentSha, String message, String author, Instant committedAt,
                                   String description, String status, Instant createdAt) {}

    public record NodeSpec(long nodeId, String code, String name, String description, List<String> tasks,
                           List<AcceptanceCriterion> acceptanceCriteria, Integer criteriaVersion,
                           String criteriaSource) {
        public NodeSpec(long nodeId, String code, String name, String description, List<String> tasks) {
            this(nodeId, code, name, description, tasks, List.of(), null, "legacy_inferred");
        }
    }

    public record RawResult(long nodeId, String decision, String summary, List<Evidence> evidence,
                            List<String> suggestions, BugDraft bugDraft, List<CriterionResult> criterionResults) {
        public RawResult(long nodeId, String decision, String summary, List<Evidence> evidence,
                         List<String> suggestions, BugDraft bugDraft) {
            this(nodeId, decision, summary, evidence, suggestions, bugDraft, List.of());
        }
    }
}
