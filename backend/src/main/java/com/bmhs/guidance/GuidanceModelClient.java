package com.bmhs.guidance;

public interface GuidanceModelClient {
    GuidanceModels.ModelReply complete(String systemPrompt, String userPrompt);
}
