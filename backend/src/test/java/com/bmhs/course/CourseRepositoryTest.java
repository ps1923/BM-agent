package com.bmhs.course;

import com.bmhs.course.CourseModels.ExperimentSummary;
import org.mockito.ArgumentMatchers;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseRepositoryTest {
    @Test
    void personalExperimentQueryFiltersByCreatorAndExcludesArchivedRecords() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ExperimentSummary experiment = new ExperimentSummary(31L, "个人实验", "说明", "draft");
        when(jdbc.query(contains("WHERE creator_id = ? AND status <> 'archived' ORDER BY updated_at DESC"),
                ArgumentMatchers.<RowMapper<ExperimentSummary>>any(), eq(7L))).thenReturn(List.of(experiment));
        CourseRepository repository = new CourseRepository(jdbc);

        assertEquals(List.of(experiment), repository.findExperimentsCreatedBy(7L));
        verify(jdbc).query(contains("WHERE creator_id = ? AND status <> 'archived' ORDER BY updated_at DESC"),
                ArgumentMatchers.<RowMapper<ExperimentSummary>>any(), eq(7L));
    }
}
