package com.bmhs.git;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GitConfig {
    private final String apiBaseUrl;
    private final String publicBaseUrl;
    private final String adminToken;
    private final String webhookSecret;
    private final String webhookTargetUrl;
    private final String credentialEncryptionKey;

    public GitConfig(@Value("${bm-hs.git.api-base-url:}") String apiBaseUrl,
                     @Value("${bm-hs.git.public-base-url:}") String publicBaseUrl,
                     @Value("${bm-hs.git.admin-token:}") String adminToken,
                     @Value("${bm-hs.git.webhook-secret:}") String webhookSecret,
                     @Value("${bm-hs.git.webhook-target-url:}") String webhookTargetUrl,
                     @Value("${bm-hs.git.credential-encryption-key:}") String credentialEncryptionKey) {
        this.apiBaseUrl = trimSlash(apiBaseUrl);
        this.publicBaseUrl = trimSlash(publicBaseUrl);
        this.adminToken = clean(adminToken);
        this.webhookSecret = clean(webhookSecret);
        this.webhookTargetUrl = clean(webhookTargetUrl);
        this.credentialEncryptionKey = clean(credentialEncryptionKey);
    }

    public String apiBaseUrl() { return apiBaseUrl; }
    public String publicBaseUrl() { return publicBaseUrl; }
    public String adminToken() { return adminToken; }
    public String webhookSecret() { return webhookSecret; }
    public String webhookTargetUrl() { return webhookTargetUrl; }
    public String credentialEncryptionKey() { return credentialEncryptionKey; }

    public boolean configured() {
        return !apiBaseUrl.isBlank() && !publicBaseUrl.isBlank() && !adminToken.isBlank()
                && !webhookSecret.isBlank() && !webhookTargetUrl.isBlank()
                && !credentialEncryptionKey.isBlank();
    }

    private static String clean(String value) { return value == null ? "" : value.trim(); }
    private static String trimSlash(String value) {
        String result = clean(value);
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }
}
