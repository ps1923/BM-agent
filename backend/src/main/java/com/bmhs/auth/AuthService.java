package com.bmhs.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.auth.AuthModels.LoginRequest;
import com.bmhs.auth.AuthModels.LoginResult;
import com.bmhs.experimentcreation.ApiException;

@Service
public class AuthService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final AuthRepository repository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final Duration sessionDuration;
    private final String dummyHash;

    public AuthService(AuthRepository repository,
                       @Value("${bm-hs.auth.session-duration:PT24H}") Duration sessionDuration) {
        this.repository = repository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        if (sessionDuration == null || sessionDuration.isZero() || sessionDuration.isNegative()) {
            throw new IllegalArgumentException("认证会话有效期必须大于 0");
        }
        this.sessionDuration = sessionDuration;
        this.dummyHash = passwordEncoder.encode("invalid-login-password");
    }

    public LoginResult login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        AuthRepository.UserRecord user = repository.findUserByEmail(email);
        String hash = user == null || user.passwordHash() == null ? dummyHash : user.passwordHash();
        boolean passwordMatches = matchesSafely(request.password(), hash);
        if (user == null || !passwordMatches || !"active".equals(user.status())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "邮箱或密码错误");
        }
        String rawToken = newToken();
        Instant expiresAt = Instant.now().plus(sessionDuration);
        repository.insertSession(user.id(), hash(rawToken), expiresAt);
        return new LoginResult(new AuthenticatedUser(user.id(), user.email(), user.displayName(), user.role()),
                rawToken, expiresAt);
    }

    public AuthenticatedUser authenticate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return null;
        return repository.findActiveUserByTokenHash(hash(rawToken));
    }

    public void logout(String rawToken) {
        if (rawToken != null && !rawToken.isBlank()) repository.revokeByTokenHash(hash(rawToken));
    }

    private boolean matchesSafely(String rawPassword, String encodedPassword) {
        try {
            return passwordEncoder.matches(rawPassword, encodedPassword);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }
}
