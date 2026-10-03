package com.bmhs.auth;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthContextTest {
    @Test
    void missingSessionIsRejectedAs401() {
        HttpServletRequest request = new MockHttpServletRequest();
        var exception = assertThrows(com.bmhs.experimentcreation.ApiException.class,
                () -> AuthContext.require(request));
        assertEquals("AUTHENTICATION_REQUIRED", exception.code());
    }

    @Test
    void studentCannotUseTeacherRole() {
        var exception = assertThrows(com.bmhs.experimentcreation.ApiException.class,
                () -> AuthContext.requireRole(new AuthenticatedUser(1, "s@example.com", "学生", "student"),
                        "teacher"));
        assertEquals("ROLE_FORBIDDEN", exception.code());
    }

    @Test
    void teacherCanUseTeacherRole() {
        var teacher = new AuthenticatedUser(2, "t@example.com", "教师", "teacher");
        AuthContext.requireRole(teacher, "teacher");
    }
}
