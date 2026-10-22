package com.bmhs.git;

import com.bmhs.experimentcreation.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GitCredentialCipherTest {
    @Test
    void encryptsAndAuthenticatesCredentialAtRest() {
        GitConfig config = config(Base64.getEncoder().encodeToString(new byte[32]));
        GitCredentialCipher cipher = new GitCredentialCipher(config);

        String encrypted = cipher.encrypt("gitea-personal-access-token");

        assertNotEquals("gitea-personal-access-token", encrypted);
        assertEquals("gitea-personal-access-token", cipher.decrypt(encrypted));
        assertNotEquals(encrypted, cipher.encrypt("gitea-personal-access-token"));
    }

    @Test
    void rejectsInvalidEncryptionKeyBeforeCredentialCanBeStored() {
        GitCredentialCipher cipher = new GitCredentialCipher(config("not-base64"));

        ApiException exception = assertThrows(ApiException.class, () -> cipher.encrypt("token"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.status());
        assertEquals("GIT_CREDENTIAL_ENCRYPTION_UNAVAILABLE", exception.code());
    }

    private GitConfig config(String key) {
        return new GitConfig("http://127.0.0.1:3000", "https://git.example.test",
                "admin-secret", "w".repeat(40), "https://api.example.test/api/integrations/gitea/webhook", key);
    }
}
