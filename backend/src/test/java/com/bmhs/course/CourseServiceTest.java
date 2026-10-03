package com.bmhs.course;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.course.CourseModels.CourseView;
import com.bmhs.course.CourseModels.ExperimentSummary;
import com.bmhs.experimentcreation.ApiException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseServiceTest {
    @Test
    void studentListingUsesOnlyStudentCourses() {
        CourseRepository repository = mock(CourseRepository.class);
        var student = new AuthenticatedUser(7L, "student@example.com", "学生", "student");
        CourseView course = new CourseView(11L, 3L, "Java", "ABCD2345", 1, List.of());
        when(repository.findCoursesForStudent(7L)).thenReturn(List.of(course));

        assertEquals(List.of(course), new CourseService(repository).list(student));
        verify(repository).findCoursesForStudent(7L);
    }

    @Test
    void personalExperimentListingIsScopedToTheAuthenticatedUser() {
        CourseRepository repository = mock(CourseRepository.class);
        var student = new AuthenticatedUser(7L, "student@example.com", "学生", "student");
        ExperimentSummary experiment = new ExperimentSummary(21L, "个人实验", "说明", "draft");
        when(repository.findExperimentsCreatedBy(7L)).thenReturn(List.of(experiment));

        assertEquals(List.of(experiment), new CourseService(repository).myExperiments(student));
        verify(repository).findExperimentsCreatedBy(7L);
    }

    @Test
    void personalExperimentListingRejectsUnsupportedRoles() {
        CourseRepository repository = mock(CourseRepository.class);
        var user = new AuthenticatedUser(7L, "user@example.com", "用户", "admin");

        ApiException exception = assertThrows(ApiException.class,
                () -> new CourseService(repository).myExperiments(user));

        assertEquals("ROLE_FORBIDDEN", exception.code());
    }

    @Test
    void teacherCannotJoinAsStudent() {
        CourseRepository repository = mock(CourseRepository.class);
        var teacher = new AuthenticatedUser(3L, "teacher@example.com", "教师", "teacher");

        ApiException exception = assertThrows(ApiException.class,
                () -> new CourseService(repository).joinByInvite(teacher, "ABCD2345"));

        assertEquals("ROLE_FORBIDDEN", exception.code());
    }

    @Test
    void courseMembersRequireTeacherOwnershipCheckedByRepositoryRow() {
        CourseRepository repository = mock(CourseRepository.class);
        var teacher = new AuthenticatedUser(3L, "teacher@example.com", "教师", "teacher");
        when(repository.findCourse(11L)).thenReturn(new CourseRepository.CourseRow(
                11L, 99L, "Other", "ABCD2345"));

        ApiException exception = assertThrows(ApiException.class,
                () -> new CourseService(repository).members(teacher, 11L));

        assertEquals("COURSE_FORBIDDEN", exception.code());
    }
}
