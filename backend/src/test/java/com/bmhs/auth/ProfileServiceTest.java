package com.bmhs.auth;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.auth.AuthModels.ProfileUpdateRequest;
import com.bmhs.auth.AuthModels.ProfileView;
import com.bmhs.experimentcreation.ApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileServiceTest {
    private final AuthRepository repository = mock(AuthRepository.class);
    private final ProfileService service = new ProfileService(repository);

    @Test
    void studentAndTeacherCanReadTheirOwnProfile() {
        AuthenticatedUser teacher = new AuthenticatedUser(4, "t@example.test", "Teacher", "teacher");
        ProfileView profile = new ProfileView(4, "t@example.test", "Teacher", "T Name", "t@git.example.test");
        when(repository.findProfile(4)).thenReturn(profile);

        assertEquals(profile, service.get(teacher));
    }

    @Test
    void updateUsesAuthenticatedIdAndNormalizesOptionalGitFields() {
        AuthenticatedUser student = new AuthenticatedUser(7, "s@example.test", "Student", "student");
        ProfileView profile = new ProfileView(7, "s@example.test", "New Name", "Git User", null);
        when(repository.updateProfile(7, "New Name", "Git User", null)).thenReturn(profile);

        assertEquals(profile, service.update(student, new ProfileUpdateRequest(" New Name ", " Git User ", " ")));
        verify(repository).updateProfile(7, "New Name", "Git User", null);
    }

    @Test
    void unsupportedRoleCannotUseProfileApi() {
        AuthenticatedUser unknown = new AuthenticatedUser(8, "x@example.test", "Unknown", "admin");

        ApiException exception = assertThrows(ApiException.class, () -> service.get(unknown));

        assertEquals("ROLE_FORBIDDEN", exception.code());
    }
}
