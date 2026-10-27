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
    void studentCannotReadCourseScopedReviewerAssignments() {
        AuthenticatedUser student = new AuthenticatedUser(7L, "s@example.test", "学生", "student");

        ApiException exception = assertThrows(ApiException.class, () -> service.reviewableExperiments(student));

        assertEquals("ROLE_FORBIDDEN", exception.code());
        verifyNoInteractions(repository);
    }

    @Test
    void teacherCanReadOnlyRepositoryScopedToTheirIdentity() {
        AuthenticatedUser teacher = new AuthenticatedUser(7L, "t@example.test", "教师", "teacher");

        service.reviewableExperiments(teacher);

        verify(repository).reviewableExperiments(7L);
    }

    @Test
    void teacherCanOnlyAssignThemselvesAsReviewer() {
        AuthenticatedUser teacher = new AuthenticatedUser(7L, "t@example.test", "教师", "teacher");

        ApiException exception = assertThrows(ApiException.class,
                () -> service.assign(teacher, 12, new ExperimentReviewerModels.AssignmentRequest(8, 2)));

        assertEquals("EXPERIMENT_REVIEW_FORBIDDEN", exception.code());
        verifyNoInteractions(repository);
    }

    @Test
    void teacherCanAssignThemselvesForTheirCourseStudentExperiment() {
        AuthenticatedUser teacher = new AuthenticatedUser(7L, "t@example.test", "教师", "teacher");

        service.assign(teacher, 12, new ExperimentReviewerModels.AssignmentRequest(7, 2));

        verify(repository).assign(12, 7L, 2L);
    }
}
