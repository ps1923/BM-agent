package com.bmhs.git;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.experimentcreation.ApiException;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.bmhs.git.GitAdminSettingsModels.SettingsRequest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class GitAdminSettingsServiceTest {
    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private final GitSettingsRepository repository = mock(GitSettingsRepository.class);
    private final GitCredentialCipher cipher = new GitCredentialCipher(KEY);
    private final AtomicReference<GitSettingsRepository.StoredSettings> stored = new AtomicReference<>();
    private final GitConfig config = new GitConfig("", "", "", "", "", KEY,
            repository, cipher);
    private final GitAdminSettingsService service = new GitAdminSettingsService(repository, config, cipher);

    GitAdminSettingsServiceTest() {
        when(repository.find()).thenAnswer(ignored -> Optional.ofNullable(stored.get()));
        doAnswer(invocation -> {
            stored.set(new GitSettingsRepository.StoredSettings(invocation.getArgument(0), invocation.getArgument(1),
                    invocation.getArgument(2), invocation.getArgument(3), invocation.getArgument(4), "2026-10-22T00:00:00Z"));
            return null;
        }).when(repository).save(anyString(), anyString(), nullable(String.class), nullable(String.class), anyString(), anyLong());
    }

    @Test
    void rejectsTeacherAndStudentBeforeReadingOrWritingSettings() {
        assertEquals(403, assertThrows(ApiException.class, () -> service.get(user("teacher"))).status().value());
        assertEquals(403, assertThrows(ApiException.class, () -> service.save(user("student"), validRequest())).status().value());
        verifyNoInteractions(repository);
    }

    @Test
    void acceptsHttpPublicUrlsForDevelopmentDemo() {
        SettingsRequest request = new SettingsRequest("http://gitea:3000", "http://git.example.com", "token-secret-123", false,
                "w".repeat(40), false, "http://school.example.com/api/integrations/gitea/webhook");

        GitAdminSettingsModels.SettingsView view = service.save(user("admin"), request);

        assertEquals("http://git.example.com", config.publicBaseUrl());
        assertEquals("http://school.example.com/api/integrations/gitea/webhook", config.webhookTargetUrl());
        assertTrue(view.configured());
    }

    @Test
    void rejectsUnsupportedPublicUrlSchemesAndRestrictsInternalApiHosts() {
        SettingsRequest unsupportedScheme = new SettingsRequest("http://gitea:3000", "ftp://git.example.com", "token-secret-123", false,
                "w".repeat(40), false, "http://school.example.com/api/integrations/gitea/webhook");
        assertEquals("GIT_SETTINGS_INVALID", assertThrows(ApiException.class,
                () -> service.save(user("admin"), unsupportedScheme)).code());
        SettingsRequest externalApi = new SettingsRequest("http://169.254.169.254/latest/meta-data", "https://git.example.test", "token-secret-123", false,
                "w".repeat(40), false, "https://school.example/api/integrations/gitea/webhook");
        assertEquals("GIT_SETTINGS_INVALID", assertThrows(ApiException.class, () -> service.save(user("admin"), externalApi)).code());
        SettingsRequest nonCanonicalLoopback = new SettingsRequest("http://gitea:3000", "https://127.1", "token-secret-123", false,
                "w".repeat(40), false, "https://school.example/api/integrations/gitea/webhook");
        assertEquals("GIT_SETTINGS_INVALID", assertThrows(ApiException.class,
                () -> service.save(user("admin"), nonCanonicalLoopback)).code());
        SettingsRequest reservedDomain = new SettingsRequest("http://gitea:3000", "https://git.example.test", "token-secret-123", false,
                "w".repeat(40), false, "https://school.example/api/integrations/gitea/webhook");
        assertEquals("GIT_SETTINGS_INVALID", assertThrows(ApiException.class,
                () -> service.save(user("admin"), reservedDomain)).code());
        SettingsRequest malformedDomain = new SettingsRequest("http://gitea:3000", "https://-git..example.com", "token-secret-123", false,
                "w".repeat(40), false, "https://school.example/api/integrations/gitea/webhook");
        assertEquals("GIT_SETTINGS_INVALID", assertThrows(ApiException.class,
                () -> service.save(user("admin"), malformedDomain)).code());
        SettingsRequest apiPath = new SettingsRequest("http://gitea:3000/elsewhere", "https://git.example.com", "token-secret-123", false,
                "w".repeat(40), false, "https://school.example.com/api/integrations/gitea/webhook");
        assertEquals("GIT_SETTINGS_INVALID", assertThrows(ApiException.class,
                () -> service.save(user("admin"), apiPath)).code());
        verify(repository, never()).save(anyString(), anyString(), nullable(String.class), nullable(String.class), anyString(), anyLong());
    }

    @Test
    void encryptsSecretsAndMakesDatabaseSettingsImmediatelyActiveWithoutReturningSecrets() {
        SettingsRequest request = validRequest();

        GitAdminSettingsModels.SettingsView view = service.save(user("admin"), request);

        GitSettingsRepository.StoredSettings row = stored.get();
        assertNotNull(row);
        assertNotEquals(request.adminToken(), row.adminTokenCiphertext());
        assertNotEquals(request.webhookSecret(), row.webhookSecretCiphertext());
        assertEquals(request.adminToken(), cipher.decrypt(row.adminTokenCiphertext()));
        assertEquals(request.webhookSecret(), cipher.decrypt(row.webhookSecretCiphertext()));
        assertEquals("http://gitea:3000", config.apiBaseUrl());
        assertEquals("http://git.example.com", config.publicBaseUrl());
        assertEquals(request.adminToken(), config.adminToken());
        assertEquals(request.webhookSecret(), config.webhookSecret());
        assertEquals("database", view.source());
        assertTrue(view.adminTokenConfigured());
        assertTrue(view.webhookSecretConfigured());
        assertFalse(view.toString().contains(request.adminToken()));
        assertFalse(view.toString().contains(request.webhookSecret()));
        assertTrue(java.util.Arrays.stream(GitAdminSettingsModels.SettingsView.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .noneMatch(name -> name.equals("adminToken") || name.equals("webhookSecret")
                        || name.toLowerCase().contains("ciphertext")));
    }

    @Test
    void blankSecretsPreserveCurrentValuesAndExplicitClearRemovesThem() {
        service.save(user("admin"), validRequest());
        GitAdminSettingsModels.SettingsView retained = service.save(user("admin"), new SettingsRequest(
                "http://gitea:3000", "https://git.example.com", "", false, "", false,
                "https://school.example.com/api/integrations/gitea/webhook"));
        assertTrue(retained.adminTokenConfigured());
        assertTrue(retained.webhookSecretConfigured());

        GitAdminSettingsModels.SettingsView cleared = service.save(user("admin"), new SettingsRequest(
                "http://gitea:3000", "https://git.example.com", "", true, "", true,
                "https://school.example.com/api/integrations/gitea/webhook"));
        assertFalse(cleared.adminTokenConfigured());
        assertFalse(cleared.webhookSecretConfigured());
        assertNull(stored.get().adminTokenCiphertext());
        assertNull(stored.get().webhookSecretCiphertext());
        assertFalse(config.configured());
    }

    @Test
    void invalidEncryptionKeyDoesNotReplaceExistingSettings() {
        GitCredentialCipher invalidCipher = new GitCredentialCipher("invalid");
        GitAdminSettingsService invalidService = new GitAdminSettingsService(repository,
                new GitConfig("", "", "", "", "", "invalid", repository, invalidCipher), invalidCipher);
        assertEquals("GIT_CREDENTIAL_ENCRYPTION_UNAVAILABLE", assertThrows(ApiException.class,
                () -> invalidService.save(user("admin"), validRequest())).code());
        verify(repository, never()).save(anyString(), anyString(), nullable(String.class), nullable(String.class), anyString(), anyLong());
    }

    private SettingsRequest validRequest() {
        return new SettingsRequest("http://gitea:3000/", "http://git.example.com/", "gitea-admin-token-123", false,
                "w".repeat(40), false, "http://school.example.com/api/integrations/gitea/webhook");
    }

    private AuthenticatedUser user(String role) { return new AuthenticatedUser(42, role + "@example.test", role, role); }
}
