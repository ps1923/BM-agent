package com.bmhs.guidance;

import com.bmhs.rag.RagModels;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class GuidanceModels {
    private GuidanceModels() {}

    public record GuidanceRequest(@NotBlank @Size(max = 6000) String question) {}

    public record GuidanceResponse(String content, String model, boolean ragAvailable,
                                   List<RagModels.RagReference> ragReferences,
                                   List<String> referencedFiles, List<String> verificationSteps) {}

    public record ExperimentContext(long experimentId, long runId, String name, String learningGoal,
                                    String description, Long nodeId, String nodeName, String nodeDescription) {}

    public record ChatMessage(String role, String content) {}

    public record ModelReply(String content, String model) {}
}
