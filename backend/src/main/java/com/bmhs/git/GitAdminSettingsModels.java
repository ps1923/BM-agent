package com.bmhs.git;

import jakarta.validation.constraints.Size;

public final class GitAdminSettingsModels {
    private GitAdminSettingsModels() {}

    public record SettingsRequest(
            @Size(max = 500) String apiBaseUrl,
            @Size(max = 500) String publicBaseUrl,
            @Size(max = 4096) String adminToken,
            boolean clearAdminToken,
            @Size(max = 4096) String webhookSecret,
            boolean clearWebhookSecret,
            @Size(max = 1000) String webhookTargetUrl) {}

    public record SettingsView(String apiBaseUrl, String publicBaseUrl, String webhookTargetUrl,
                               boolean adminTokenConfigured, boolean webhookSecretConfigured,
                               boolean configured, String source, String updatedAt) {}
}
