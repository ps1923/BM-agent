package com.bmhs.teacher;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.experimentcreation.ApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ExperimentReviewerServiceTest {
    private final ExperimentReviewerRepository repository = mock(ExperimentReviewerRepository.class);
    private final ExperimentReviewerService service = new ExperimentReviewerService(repository);

    @Test
    void reviewerAssignmentListsAreAdminOnly() {
        AuthenticatedUser teacher = new AuthenticatedUser(7L, "t@example.test", "教师", "teacher");

        ApiException experiments = assertThrows(ApiException.class, () -> service.reviewableExperiments(teacher));
        ApiException teachers = assertThrows(ApiException.class, () -> service.activeTeachers(teacher));

        assertEquals("ROLE_FORBIDDEN", experiments.code());
        assertEquals("ROLE_FORBIDDEN", teachers.code());
        verifyNoInteractions(repository);
    }

    @Test
    void adminCanReadAssignableExperimentsAndActiveTeachers() {
        AuthenticatedUser admin = new AuthenticatedUser(1L, "a@example.test", "管理员", "admin");

        service.reviewableExperiments(admin);
        service.activeTeachers(admin);

        verify(repository).reviewableExperiments();
        verify(repository).activeTeachers();
    }
}
