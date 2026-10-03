package com.bmhs.auth;

import com.bmhs.auth.AuthModels.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    @Test
    void loginStoresOnlyAHashOfTheOpaqueToken() {
        AuthRepository repository = mock(AuthRepository.class);
        String passwordHash = new BCryptPasswordEncoder().encode("correct-password");
        when(repository.findUserByEmail("student@example.com"))
                .thenReturn(new AuthRepository.UserRecord(7L, "student@example.com", "学生", "student",
                        "active", passwordHash));

        AuthModels.LoginResult result = new AuthService(repository, Duration.ofHours(2))
                .login(new LoginRequest("student@example.com", "correct-password"));

        assertTrue(result.rawToken().length() > 20);
        assertNotEquals(result.rawToken(), result.user().email());
        verify(repository).insertSession(eq(7L), any(String.class), any());
    }

    @Test
    void invalidOrDisabledAccountsUseTheSameAuthenticationError() {
        AuthRepository repository = mock(AuthRepository.class);
        String passwordHash = new BCryptPasswordEncoder().encode("correct-password");
        when(repository.findUserByEmail("disabled@example.com"))
                .thenReturn(new AuthRepository.UserRecord(8L, "disabled@example.com", "停用用户", "student",
                        "disabled", passwordHash));
        AuthService service = new AuthService(repository, Duration.ofHours(2));

        assertThrows(com.bmhs.experimentcreation.ApiException.class,
                () -> service.login(new LoginRequest("disabled@example.com", "correct-password")));
    }

    @Test
    void logoutRevokesTheHashedOpaqueToken() {
        AuthRepository repository = mock(AuthRepository.class);
        new AuthService(repository, Duration.ofHours(2)).logout("opaque-token");

        verify(repository).revokeByTokenHash(any(String.class));
    }
}
