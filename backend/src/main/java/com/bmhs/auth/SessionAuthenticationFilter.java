package com.bmhs.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import com.bmhs.auth.AuthModels.AuthenticatedUser;

@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {
    private final AuthService authService;
    private final String cookieName;

    public SessionAuthenticationFilter(AuthService authService,
                                       @Value("${bm-hs.auth.cookie-name:BM_SESSION}") String cookieName) {
        this.authService = authService;
        this.cookieName = cookieName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }
        String rawToken = token(request);
        if (rawToken != null) {
            AuthenticatedUser user = authService.authenticate(rawToken);
            if (user != null) request.setAttribute(AuthContext.REQUEST_ATTRIBUTE, user);
        }
        filterChain.doFilter(request, response);
    }

    private String token(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }
}
