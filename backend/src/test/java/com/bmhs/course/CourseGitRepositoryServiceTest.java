package com.bmhs.course;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.course.CourseModels.CourseGitRepositoryRequest;
import com.bmhs.experimentcreation.ApiException;
import com.bmhs.git.GitConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseGitRepositoryServiceTest {
    private final CourseRepository repository = mock(CourseRepository.class);
    private final CourseGitRepositoryService service = new CourseGitRepositoryService(repository,
            new GitConfig("http://gitea:3000", "http://git.example.edu", "token", "w".repeat(32),
                    "http://app.example.edu/webhook", "e".repeat(32)));
    private final AuthenticatedUser teacher = new AuthenticatedUser(3, "t@example.test", "Teacher", "teacher");

    @Test
    void courseOwnerCanSaveRepositoryOnConfiguredGiteaHost() {
        when(repository.findCourse(12)).thenReturn(new CourseRepository.CourseRow(12, 3, "Java", "ABC12345"));

        var result = service.update(teacher, 12,
                new CourseGitRepositoryRequest("http://git.example.edu/course/java-course.git"));

        assertEquals("http://git.example.edu/course/java-course", result.repositoryUrl());
        verify(repository).updateGitRepositoryUrl(12, "http://git.example.edu/course/java-course");
    }

    @Test
    void anotherTeachersCourseIsNotReadableOrWritable() {
        when(repository.findCourse(12)).thenReturn(new CourseRepository.CourseRow(12, 99, "Other", "ABC12345"));

        ApiException exception = assertThrows(ApiException.class, () -> service.get(teacher, 12));

        assertEquals("COURSE_FORBIDDEN", exception.code());
    }

    @Test
    void repositoryOnAnUnrelatedHostIsRejected() {
        when(repository.findCourse(12)).thenReturn(new CourseRepository.CourseRow(12, 3, "Java", "ABC12345"));

        ApiException exception = assertThrows(ApiException.class, () -> service.update(teacher, 12,
                new CourseGitRepositoryRequest("https://evil.example.org/course/repo")));

        assertEquals("COURSE_GIT_REPOSITORY_INVALID", exception.code());
    }

    @Test
    void nonTeacherCannotConfigureCourseRepository() {
        AuthenticatedUser student = new AuthenticatedUser(4, "s@example.test", "Student", "student");

        ApiException exception = assertThrows(ApiException.class, () -> service.update(student, 12,
                new CourseGitRepositoryRequest("http://git.example.edu/course/repo")));

        assertEquals("ROLE_FORBIDDEN", exception.code());
    }
}
