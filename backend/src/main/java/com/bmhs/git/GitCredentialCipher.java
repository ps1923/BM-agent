package com.bmhs.git;

import com.bmhs.experimentcreation.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class GitCredentialCipher {
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private final GitConfig config;
    private final SecureRandom random = new SecureRandom();

    public GitCredentialCipher(GitConfig config) { this.config = config; }

    public String encrypt(String plaintext) {
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, nonce));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(nonce.length + encrypted.length)
                    .put(nonce).put(encrypted).array());
        } catch (Exception exception) {
            throw unavailable();
        }
    }

    public String decrypt(String ciphertext) {
        try {
            byte[] packed = Base64.getDecoder().decode(ciphertext);
            if (packed.length <= NONCE_BYTES) throw unavailable();
            ByteBuffer buffer = ByteBuffer.wrap(packed);
            byte[] nonce = new byte[NONCE_BYTES];
            buffer.get(nonce);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, nonce));
            return new String(cipher.doFinal(encrypted), java.nio.charset.StandardCharsets.UTF_8);
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw unavailable();
        }
    }

    private SecretKeySpec key() {
        try {
            byte[] bytes = Base64.getDecoder().decode(config.credentialEncryptionKey());
            if (bytes.length != 32) throw new IllegalArgumentException("Git credential key must be 32 bytes");
            return new SecretKeySpec(bytes, "AES");
        } catch (IllegalArgumentException exception) {
            throw unavailable();
        }
    }

    private ApiException unavailable() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "GIT_CREDENTIAL_ENCRYPTION_UNAVAILABLE",
                "Git 凭据安全配置无效，无法安全发放仓库凭据");
    }
}
