package com.bmhs.auth;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.auth.AuthModels.LoginRequest;
import com.bmhs.auth.AuthModels.LoginResponse;
import com.bmhs.auth.AuthModels.LoginResult;
import jakarta.validation.Valid;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final String cookieName;
    private final boolean secureCookie;
    private final String sameSite;
    private final Duration sessionDuration;

    public AuthController(AuthService authService,
                          @Value("${bm-hs.auth.cookie-name:BM_SESSION}") String cookieName,
                          @Value("${bm-hs.auth.secure-cookie:false}") boolean secureCookie,
                          @Value("${bm-hs.auth.same-site:Lax}") String sameSite,
                          @Value("${bm-hs.auth.session-duration:PT24H}") Duration sessionDuration) {
        this.authService = authService;
        this.cookieName = cookieName;
        this.secureCookie = secureCookie;
        this.sameSite = sameSite;
        this.sessionDuration = sessionDuration;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = authService.login(request);
        return ResponseEntity.ok().header("Set-Cookie", cookie(result.rawToken(), sessionDuration))
                .body(new LoginResponse(result.user(), result.expiresAt()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) token = cookie.getValue();
                if (token != null) break;
            }
        }
        authService.logout(token);
        return ResponseEntity.noContent().header("Set-Cookie", cookie("", Duration.ZERO)).build();
    }

    @GetMapping("/me")
    public AuthenticatedUser me(AuthenticatedUser user) {
        return user;
    }

    private String cookie(String value, Duration maxAge) {
        return ResponseCookie.from(cookieName, value)
                .httpOnly(true).secure(secureCookie).sameSite(sameSite).path("/")
                .maxAge(maxAge)
                .build().toString();
    }
}
