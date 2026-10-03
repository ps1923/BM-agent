package com.bmhs.auth;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.auth.AuthModels.LoginRequest;
import com.bmhs.auth.AuthModels.LoginResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {
    @Test
    void loginSetsAnHttpOnlySessionCookie() {
        AuthService service = mock(AuthService.class);
        when(service.login(new LoginRequest("s@example.com", "password-123")))
                .thenReturn(new LoginResult(new AuthenticatedUser(1, "s@example.com", "学生", "student"),
                        "opaque-token", Instant.now().plus(Duration.ofHours(1))));
        AuthController controller = new AuthController(service, "BM_SESSION", false, "Lax", Duration.ofHours(1));

        ResponseEntity<?> response = controller.login(new LoginRequest("s@example.com", "password-123"));

        String setCookie = response.getHeaders().getFirst("Set-Cookie");
        assertTrue(setCookie.startsWith("BM_SESSION=opaque-token"));
        assertTrue(setCookie.contains("HttpOnly"));
    }

    @Test
    void secureCookieModeAddsSecureHttpOnlyAndSameSiteAttributes() {
        AuthService service = mock(AuthService.class);
        when(service.login(new LoginRequest("s@example.com", "password-123")))
                .thenReturn(new LoginResult(new AuthenticatedUser(1, "s@example.com", "学生", "student"),
                        "opaque-token", Instant.now().plus(Duration.ofHours(1))));
        AuthController controller = new AuthController(service, "BM_SESSION", true, "Lax", Duration.ofHours(1));

        ResponseEntity<?> response = controller.login(new LoginRequest("s@example.com", "password-123"));

        String setCookie = response.getHeaders().getFirst("Set-Cookie");
        assertTrue(setCookie.contains("Secure"));
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("SameSite=Lax"));
    }
}
