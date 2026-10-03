package com.bmhs.rag;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.experimentcreation.ApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

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
}
