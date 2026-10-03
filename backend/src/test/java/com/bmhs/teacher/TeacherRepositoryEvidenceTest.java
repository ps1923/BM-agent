package com.bmhs.teacher;

import com.bmhs.experimentcreation.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class TeacherRepositoryEvidenceTest {
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void rejectsUnauthorizedRunBeforeLookingUpSnapshotFiles() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(contains("course_experiment_assignments"), any(RowMapper.class), eq(3L), eq(8L)))
                .thenReturn(java.util.List.of());
        TeacherRepository repository = new TeacherRepository(jdbc);

        ApiException exception = assertThrows(ApiException.class,
                () -> repository.assertSnapshotAccess(3L, 8L, 12L));

        assertEquals("RUN_FORBIDDEN", exception.code());
        verify(jdbc).query(contains("course_experiment_assignments"), any(RowMapper.class), eq(3L), eq(8L));
        verifyNoMoreInteractions(jdbc);
    }
}
