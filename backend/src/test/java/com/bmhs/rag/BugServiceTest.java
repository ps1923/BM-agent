package com.bmhs.rag;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.experimentcreation.ApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BugServiceTest {
    @Test
    void teacherCannotCreateStudentBugCase() {
        BugService service = new BugService(mock(BugRepository.class), mock(RagService.class));
        AuthenticatedUser teacher = new AuthenticatedUser(3L, "teacher@example.com", "教师", "teacher");

        ApiException exception = assertThrows(ApiException.class,
                () -> service.create(teacher, 11L, new RagModels.BugRequest("标题", "问题", "解决", null)));

        assertEquals("ROLE_FORBIDDEN", exception.code());
    }

    @Test
    void studentListDoesNotAcceptTeacherCourseFilter() {
        BugRepository repository = mock(BugRepository.class);
        BugService service = new BugService(repository, mock(RagService.class));
        AuthenticatedUser student = new AuthenticatedUser(7L, "student@example.com", "学生", "student");

        service.list(student, 99L);

        org.mockito.Mockito.verify(repository).findVisibleForStudent(7L);
    }

    @Test
    void onlyApprovedTeacherReviewStartsVectorIndexing() {
        BugRepository repository = mock(BugRepository.class);
        RagService rag = mock(RagService.class);
        BugService service = new BugService(repository, rag);
        AuthenticatedUser teacher = new AuthenticatedUser(3L, "teacher@example.com", "教师", "teacher");
        java.time.Instant now = java.time.Instant.now();
        RagModels.BugCaseView approved = new RagModels.BugCaseView(21L, 5L, 11L, 8L, 4L, 7L,
                "依赖冲突", "启动时报错", "固定版本后启动", "Python", "approved", "course",
                "not_indexed", null, now, now);
        when(repository.review(eq(21L), eq(3L), org.mockito.ArgumentMatchers.any())).thenReturn(approved);
        when(repository.findById(21L)).thenReturn(approved);

        service.review(teacher, 21L, new RagModels.BugReviewRequest("approved", "验证通过"));

        verify(rag).indexApproved(approved);
    }

    @Test
    void rejectingIndexedCaseRemovesItFromChroma() {
        BugRepository repository = mock(BugRepository.class);
        RagService rag = mock(RagService.class);
        BugService service = new BugService(repository, rag);
        AuthenticatedUser teacher = new AuthenticatedUser(3L, "teacher@example.com", "教师", "teacher");
        java.time.Instant now = java.time.Instant.now();
        RagModels.BugCaseView rejected = new RagModels.BugCaseView(21L, 5L, 11L, 8L, 4L, 7L,
                "依赖冲突", "启动时报错", "固定版本后启动", "Python", "rejected", "personal",
                "failed", "bug-21", now, now);
        when(repository.review(eq(21L), eq(3L), org.mockito.ArgumentMatchers.any())).thenReturn(rejected);
        when(repository.findById(21L)).thenReturn(rejected);

        service.review(teacher, 21L, new RagModels.BugReviewRequest("rejected", "证据不足"));

        verify(rag).removeRejectedIndex(rejected);
    }
}
