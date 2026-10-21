package com.bmhs.dsh;

import com.bmhs.experimentcreation.ApiException;
import com.bmhs.rag.BugService;
import com.bmhs.rag.RagService;
import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.teacher.TeacherModels;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

class DshPluginServiceTest {
    private final DshPluginRepository repository = mock(DshPluginRepository.class);
    private final BugService bugService = mock(BugService.class);
    private final RagService ragService = mock(RagService.class);
    private final DshPluginService service = new DshPluginService(repository, bugService, ragService);
    private final DshPrincipal principal = new DshPrincipal(4L, 9L, Instant.now().plusSeconds(60));

    @Test
    void rejectsAbsoluteAndTraversalFilePathsBeforePersistence() {
        when(repository.assertBoundRun(principal, 12L)).thenReturn(3L);
        DshPluginModels.CodeSubmissionRequest request = new DshPluginModels.CodeSubmissionRequest(null, null,
                List.of(new DshPluginModels.SubmittedFile("../outside.py", "print('no')", "0".repeat(64))));

        ApiException error = assertThrows(ApiException.class, () -> service.submitCode(principal, 12L, request));

        assertEquals("INVALID_SUBMISSION_PATH", error.code());
    }

    @Test
    void rejectsSensitiveFilesAndContent() {
        when(repository.assertBoundRun(principal, 12L)).thenReturn(3L);
        DshPluginModels.CodeSubmissionRequest fileRequest = new DshPluginModels.CodeSubmissionRequest(null, null,
                List.of(new DshPluginModels.SubmittedFile(".env.local", "API_KEY=not-for-teacher", "0".repeat(64))));
        DshPluginModels.CodeSubmissionRequest contentRequest = new DshPluginModels.CodeSubmissionRequest(null, null,
                List.of(new DshPluginModels.SubmittedFile("settings.py", "api_key = 'sk-123456789012345678901234'", "0".repeat(64))));

        assertEquals("SENSITIVE_FILE_REJECTED", assertThrows(ApiException.class,
                () -> service.submitCode(principal, 12L, fileRequest)).code());
        assertEquals("SENSITIVE_CONTENT_REJECTED", assertThrows(ApiException.class,
                () -> service.submitCode(principal, 12L, contentRequest)).code());
    }

    @Test
    void doesNotAllowModelToMarkProgressWithoutExplicitStudentConfirmation() {
        DshPluginModels.ProgressUpdateRequest request = new DshPluginModels.ProgressUpdateRequest("completed", false);
        when(repository.assertBoundRun(principal, 12L)).thenReturn(3L);

        ApiException error = assertThrows(ApiException.class, () -> service.updateNode(principal, 12L, 8L, request));

        assertEquals("STUDENT_CONFIRMATION_REQUIRED", error.code());
    }

    @Test
    void stageReviewsAreTeacherOnly() {
        AuthenticatedUser student = new AuthenticatedUser(4L, "s@example.test", "学生", "student");
        TeacherModels.ReviewRequest request = new TeacherModels.ReviewRequest(80, "完成良好");

        ApiException error = assertThrows(ApiException.class,
                () -> service.saveTeacherDshReview(student, 12L, request));

        assertEquals("ROLE_FORBIDDEN", error.code());
        verifyNoInteractions(repository);
    }
}
