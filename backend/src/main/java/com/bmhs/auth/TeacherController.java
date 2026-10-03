package com.bmhs.auth;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {
    @GetMapping("/me")
    public AuthenticatedUser me(AuthenticatedUser user) {
        AuthContext.requireRole(user, "teacher");
        return user;
    }
}
