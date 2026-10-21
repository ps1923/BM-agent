package com.bmhs.dsh;

import com.bmhs.experimentcreation.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;

@Service
public class DshTokenService {
    private static final Duration TOKEN_TTL = Duration.ofMinutes(15);
    private static final Duration REFRESH_TTL = Duration.ofDays(30);
    private static final Pattern TOKEN_FORMAT = Pattern.compile("[A-Za-z0-9_-]{40,64}");
    private final SecureRandom secureRandom = new SecureRandom();
    private final DshTokenRepository repository;

    public DshTokenService(DshTokenRepository repository) {
        this.repository = repository;
    }

    public IssuedToken issue(long studentId, long instanceId) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = Instant.now().plus(TOKEN_TTL);
        repository.save(hash(token), studentId, instanceId, expiresAt);
        return new IssuedToken(token, expiresAt);
    }

    @Transactional
    public CredentialPair issuePair(long studentId, long instanceId) {
        IssuedToken access = issue(studentId, instanceId);
        return new CredentialPair(access, newRefreshCredential(studentId, instanceId));
    }

    @Transactional
    public CredentialPair refresh(String refreshCredential) {
        if (refreshCredential == null || !TOKEN_FORMAT.matcher(refreshCredential).matches()) throw unauthorized();
        DshPrincipal principal = repository.consumeRefresh(hash(refreshCredential), Instant.now());
        if (principal == null) throw unauthorized();
        return issuePair(principal.studentId(), principal.instanceId());
    }

    private String newRefreshCredential(long studentId, long instanceId) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        repository.saveRefresh(hash(token), studentId, instanceId, Instant.now().plus(REFRESH_TTL));
        return token;
    }

    public DshPrincipal authenticateHeader(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) throw unauthorized();
        String token = authorization.substring("Bearer ".length()).trim();
        if (!TOKEN_FORMAT.matcher(token).matches()) throw unauthorized();
        DshPrincipal principal = repository.authenticate(hash(token), Instant.now());
        if (principal == null) throw unauthorized();
        return principal;
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private ApiException unauthorized() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "DSH_CREDENTIAL_INVALID", "DSH 插件凭据无效或已过期");
    }

    public record IssuedToken(String token, Instant expiresAt) {}
    public record CredentialPair(IssuedToken access, String refreshCredential) {}
}
