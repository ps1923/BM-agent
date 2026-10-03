package com.bmhs.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AuthModels {
    private AuthModels() {}

    public record LoginRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 8, max = 200) String password) {}

    public record AuthenticatedUser(long id, String email, String displayName, String role) {}

    public record LoginResponse(AuthenticatedUser user, Instant expiresAt) {}

    public record LoginResult(AuthenticatedUser user, String rawToken, Instant expiresAt) {}
}
