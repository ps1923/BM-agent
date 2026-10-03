package com.bmhs.auth;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CurrentUserControllerTest {
    @Test
    void exposesThePublicCurrentUserContract() {
        AuthenticatedUser user = new AuthenticatedUser(3L, "teacher@example.com", "教师", "teacher");
        assertEquals(user, new CurrentUserController().me(user));
    }
}
