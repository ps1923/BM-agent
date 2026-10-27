package com.bmhs.teacher;

import com.bmhs.experimentcreation.CreationModels.AcceptanceCriterion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class NodeAcceptanceCriteriaModels {
    private NodeAcceptanceCriteriaModels() {}

    public record UpdateRequest(@NotEmpty @Size(max = 30) List<@Valid CriterionRequest> criteria) {}

    public record CriterionRequest(@NotBlank @Size(max = 500) String criterion,
                                   @NotBlank @Size(max = 30) String verificationMethod,
                                   @NotBlank @Size(max = 1000) String expectedEvidence) {
        public AcceptanceCriterion toCriterion() {
            return new AcceptanceCriterion(criterion.trim(), verificationMethod, expectedEvidence.trim());
        }
    }

    public record RevisionView(long experimentId, long nodeId, int version, String source,
                               List<AcceptanceCriterion> criteria, Long updatedBy, Instant updatedAt) {}

    public record NodeView(long experimentId, long nodeId, String nodeCode, String nodeName, int version,
                           String source, List<AcceptanceCriterion> criteria) {}
}
