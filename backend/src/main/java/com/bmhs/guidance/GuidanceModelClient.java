package com.bmhs.guidance;

public interface GuidanceModelClient {
    GuidanceModels.ModelReply complete(String systemPrompt, String userPrompt);

    default GuidanceModels.ModelReply completeJson(String systemPrompt, String userPrompt) {
        return complete(systemPrompt, userPrompt);
    }
}
