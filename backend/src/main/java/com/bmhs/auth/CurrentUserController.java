package com.bmhs.auth;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Compatibility endpoint matching the public authentication contract. */
@RestController
@RequestMapping("/api")
public class CurrentUserController {
    @GetMapping("/me")
    public AuthenticatedUser me(AuthenticatedUser user) {
        return user;
    }
}
