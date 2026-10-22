package com.bmhs.git;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GitConfig {
    private final String envApiBaseUrl;
    private final String envPublicBaseUrl;
    private final String envAdminToken;
    private final String envWebhookSecret;
    private final String envWebhookTargetUrl;
    private final String credentialEncryptionKey;
    private final GitSettingsRepository repository;
    private final GitCredentialCipher cipher;

    @Autowired
    public GitConfig(@Value("${bm-hs.git.api-base-url:}") String apiBaseUrl,
                     @Value("${bm-hs.git.public-base-url:}") String publicBaseUrl,
                     @Value("${bm-hs.git.admin-token:}") String adminToken,
                     @Value("${bm-hs.git.webhook-secret:}") String webhookSecret,
                     @Value("${bm-hs.git.webhook-target-url:}") String webhookTargetUrl,
                     @Value("${bm-hs.git.credential-encryption-key:}") String credentialEncryptionKey,
                     GitSettingsRepository repository, GitCredentialCipher cipher) {
        this.envApiBaseUrl = trimSlash(apiBaseUrl);
        this.envPublicBaseUrl = trimSlash(publicBaseUrl);
        this.envAdminToken = clean(adminToken);
        this.envWebhookSecret = clean(webhookSecret);
        this.envWebhookTargetUrl = clean(webhookTargetUrl);
        this.credentialEncryptionKey = clean(credentialEncryptionKey);
        this.repository = repository;
        this.cipher = cipher;
    }

    public GitConfig(String apiBaseUrl, String publicBaseUrl, String adminToken, String webhookSecret,
                     String webhookTargetUrl, String credentialEncryptionKey) {
        this(apiBaseUrl, publicBaseUrl, adminToken, webhookSecret, webhookTargetUrl,
                credentialEncryptionKey, null, null);
    }

    public String apiBaseUrl() { return settings().apiBaseUrl(); }
    public String publicBaseUrl() { return settings().publicBaseUrl(); }
    public String adminToken() { return settings().adminToken(); }
    public String webhookSecret() { return settings().webhookSecret(); }
    public String webhookTargetUrl() { return settings().webhookTargetUrl(); }
    public String credentialEncryptionKey() { return credentialEncryptionKey; }
    public boolean databaseSettingsPresent() { return databaseSettings().isPresent(); }
    public boolean configured() {
        Settings values = settings();
        return !values.apiBaseUrl().isBlank() && !values.publicBaseUrl().isBlank()
                && !values.adminToken().isBlank() && !values.webhookSecret().isBlank()
                && !values.webhookTargetUrl().isBlank() && !credentialEncryptionKey.isBlank();
    }

    private Settings settings() {
        return databaseSettings().map(stored -> new Settings(trimSlash(stored.apiBaseUrl()),
                trimSlash(stored.publicBaseUrl()), decrypt(stored.adminTokenCiphertext()),
                decrypt(stored.webhookSecretCiphertext()), clean(stored.webhookTargetUrl())))
                .orElseGet(() -> new Settings(envApiBaseUrl, envPublicBaseUrl, envAdminToken,
                        envWebhookSecret, envWebhookTargetUrl));
    }

    private java.util.Optional<GitSettingsRepository.StoredSettings> databaseSettings() {
        return repository == null ? java.util.Optional.empty() : repository.find();
    }

    private String decrypt(String ciphertext) {
        return ciphertext == null || ciphertext.isBlank() ? "" : cipher.decrypt(ciphertext);
    }

    private static String clean(String value) { return value == null ? "" : value.trim(); }
    private static String trimSlash(String value) {
        String result = clean(value);
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }

    private record Settings(String apiBaseUrl, String publicBaseUrl, String adminToken,
                            String webhookSecret, String webhookTargetUrl) {}
}
