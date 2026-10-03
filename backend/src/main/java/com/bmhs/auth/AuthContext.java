package com.bmhs.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.experimentcreation.ApiException;

public final class AuthContext {
    public static final String REQUEST_ATTRIBUTE = AuthContext.class.getName() + ".user";

    private AuthContext() {}

    public static AuthenticatedUser require(HttpServletRequest request) {
        Object value = request.getAttribute(REQUEST_ATTRIBUTE);
        if (value instanceof AuthenticatedUser user) return user;
        throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "请先登录");
    }

    public static void requireRole(AuthenticatedUser user, String role) {
        if (user == null || !role.equals(user.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ROLE_FORBIDDEN", "当前用户无权访问该接口");
        }
    }
}
