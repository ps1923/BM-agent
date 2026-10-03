package com.bmhs.workspace;

import com.bmhs.experimentcreation.ApiException;
import com.bmhs.workspace.WorkspaceModels.NodeProgressView;
import com.bmhs.workspace.WorkspaceModels.RunProgressView;
import com.bmhs.workspace.WorkspaceModels.RunRecord;
import com.bmhs.workspace.WorkspaceModels.WorkspaceRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkspaceRepositoryTest {
    @Test
    void workspaceProvisioningCanOnlyBeClaimedOnce() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(contains("status = 'starting'"), eq(41L))).thenReturn(1, 0);
        WorkspaceRepository repository = new WorkspaceRepository(jdbc);

        assertTrue(repository.claimWorkspaceProvisioning(41L));
        assertFalse(repository.claimWorkspaceProvisioning(41L));
    }

    @Test
    void creatorCanRunTheirPrivateDraftWithoutCourseAssignment() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(contains("creator_id = ? AND status = 'draft'"), ArgumentMatchers.<RowMapper<Integer>>any(),
                eq(31L), eq(7L))).thenReturn(List.of(1));
        WorkspaceRepository repository = new WorkspaceRepository(jdbc);

        assertTrue(repository.hasRunnableAccess(31L, 7L));
        verify(jdbc).query(contains("creator_id = ? AND status = 'draft'"), ArgumentMatchers.<RowMapper<Integer>>any(),
                eq(31L), eq(7L));
    }

    @Test
    void anotherStudentCannotRunPrivateDraft() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(contains("creator_id = ? AND status = 'draft'"), ArgumentMatchers.<RowMapper<Integer>>any(),
                eq(31L), eq(8L))).thenReturn(List.of());
        when(jdbc.query(contains("course_experiment_assignments"), ArgumentMatchers.<RowMapper<Integer>>any(),
                eq(31L), eq(8L))).thenReturn(List.of());
        WorkspaceRepository repository = new WorkspaceRepository(jdbc);

        assertFalse(repository.hasRunnableAccess(31L, 8L));
    }

    @Test
    void publishedExperimentStillRequiresActiveCourseMembership() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(contains("creator_id = ? AND status = 'draft'"), ArgumentMatchers.<RowMapper<Integer>>any(),
                eq(31L), eq(8L))).thenReturn(List.of());
        when(jdbc.query(contains("course_experiment_assignments"), ArgumentMatchers.<RowMapper<Integer>>any(),
                eq(31L), eq(8L))).thenReturn(List.of(1));
        WorkspaceRepository repository = new WorkspaceRepository(jdbc);

        assertTrue(repository.hasRunnableAccess(31L, 8L));
    }

    @Test
    void nodeCompletionIsRejectedWhileAnyDeclaredTaskIsIncomplete() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        RunRecord run = runRecord();
        when(jdbc.query(contains("FROM experiment_runs r JOIN coding_workspaces"),
                ArgumentMatchers.<RowMapper<RunRecord>>any(), eq(12L), eq(7L))).thenReturn(List.of(run));
        when(jdbc.query(contains("FOR UPDATE"), ArgumentMatchers.<RowMapper<NodeProgressView>>any(),
                eq(12L), eq(5L), eq(31L))).thenReturn(List.of(new NodeProgressView(31L, "in_progress",
                java.math.BigDecimal.valueOf(50), null, null)));
        when(jdbc.query(contains("SELECT COALESCE(tp.status, 'missing')"),
                ArgumentMatchers.<RowMapper<String>>any(), eq(12L), eq(5L), eq(31L)))
                .thenReturn(List.of("pending"));
        WorkspaceRepository repository = new WorkspaceRepository(jdbc);

        ApiException exception = assertThrows(ApiException.class, () -> repository.updateNodeProgress(
                12L, 7L, 31L, "completed", java.math.BigDecimal.valueOf(100)));

        assertEquals("NODE_TASKS_INCOMPLETE", exception.code());
        verify(jdbc).update(contains("INSERT IGNORE INTO task_progress"), eq(12L), eq(5L));
    }

    @Test
    void inProgressNodePercentageIsDerivedFromPersistedTaskStates() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(contains("FROM experiment_runs r JOIN coding_workspaces"),
                ArgumentMatchers.<RowMapper<RunRecord>>any(), eq(12L), eq(7L))).thenReturn(List.of(runRecord()));
        when(jdbc.query(contains("FROM node_progress"), ArgumentMatchers.<RowMapper<NodeProgressView>>any(),
                eq(12L), eq(5L), eq(31L))).thenReturn(List.of(new NodeProgressView(31L, "pending",
                java.math.BigDecimal.ZERO, null, null)));
        when(jdbc.query(contains("SELECT COALESCE(tp.status, 'missing')"),
                ArgumentMatchers.<RowMapper<String>>any(), eq(12L), eq(5L), eq(31L)))
                .thenReturn(List.of("completed", "pending"));
        WorkspaceRepository repository = new WorkspaceRepository(jdbc);

        repository.updateNodeProgress(12L, 7L, 31L, "in_progress", java.math.BigDecimal.valueOf(99));

        verify(jdbc).update(contains("SET status = 'in_progress', progress_percent = ?"),
                eq(new java.math.BigDecimal("50.00")), eq(12L), eq(5L), eq(31L));
    }

    @Test
    void nodeWithoutDeclaredTasksCanCompleteAndUnlockItsChildren() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        RunRecord run = runRecord();
        when(jdbc.query(contains("FROM experiment_runs r JOIN coding_workspaces"),
                ArgumentMatchers.<RowMapper<RunRecord>>any(), eq(12L), eq(7L))).thenReturn(List.of(run));
        when(jdbc.query(contains("FOR UPDATE"), ArgumentMatchers.<RowMapper<NodeProgressView>>any(),
                eq(12L), eq(5L), eq(31L))).thenReturn(List.of(new NodeProgressView(31L, "in_progress",
                java.math.BigDecimal.ZERO, null, null)));
        when(jdbc.query(contains("SELECT COALESCE(tp.status, 'missing')"),
                ArgumentMatchers.<RowMapper<String>>any(), eq(12L), eq(5L), eq(31L))).thenReturn(List.of());
        when(jdbc.query(contains("SELECT COUNT(*) FROM node_progress"),
                ArgumentMatchers.<ResultSetExtractor<Integer>>any(), eq(12L), eq(5L))).thenReturn(1);
        WorkspaceRepository repository = spy(new WorkspaceRepository(jdbc));
        RunProgressView expected = new RunProgressView(12L, 5L, "in_progress", null, List.of(), List.of());
        doReturn(expected).when(repository).findProgress(12L, 7L);

        RunProgressView actual = repository.updateNodeProgress(12L, 7L, 31L, "completed",
                java.math.BigDecimal.valueOf(100));

        assertEquals(expected, actual);
        verify(jdbc).update(contains("SET status = 'completed', progress_percent = 100"), eq(12L), eq(5L), eq(31L));
        verify(jdbc).update(contains("UPDATE node_progress child"), eq(12L), eq(5L), eq(31L));
        verify(jdbc).update(contains("SET status = 'in_progress', current_node_id = NULL"), eq(12L), eq(7L));
    }

    @Test
    void progressReadIncludesPersistedTaskIdentifiersAndStatuses() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(contains("FROM experiment_runs r JOIN coding_workspaces"),
                ArgumentMatchers.<RowMapper<RunRecord>>any(), eq(12L), eq(7L))).thenReturn(List.of(runRecord()));
        when(jdbc.query(contains("SELECT node_id, status, progress_percent"),
                ArgumentMatchers.<RowMapper<NodeProgressView>>any(), eq(12L), eq(5L)))
                .thenReturn(List.of(new NodeProgressView(31L, "in_progress", java.math.BigDecimal.valueOf(50), null, null)));
        when(jdbc.query(contains("SELECT tp.task_id, nt.node_id"),
                ArgumentMatchers.<RowMapper<WorkspaceModels.TaskProgressView>>any(), eq(12L), eq(5L)))
                .thenReturn(List.of(new WorkspaceModels.TaskProgressView(22L, 31L, "completed", null)));
        WorkspaceRepository repository = new WorkspaceRepository(jdbc);

        RunProgressView progress = repository.findProgress(12L, 7L);

        assertEquals(List.of(new WorkspaceModels.TaskProgressView(22L, 31L, "completed", null)), progress.tasks());
        verify(jdbc).update(contains("INSERT IGNORE INTO task_progress"), eq(12L), eq(5L));
    }

    @Test
    void lockedTaskCannotBeUpdated() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(contains("FROM experiment_runs r JOIN coding_workspaces"),
                ArgumentMatchers.<RowMapper<RunRecord>>any(), eq(12L), eq(7L))).thenReturn(List.of(runRecord()));
        when(jdbc.query(contains("FROM node_tasks nt"), ArgumentMatchers.<RowMapper<NodeProgressView>>any(),
                eq(12L), eq(22L), eq(5L))).thenReturn(List.of(new NodeProgressView(31L, "locked",
                java.math.BigDecimal.ZERO, null, null)));
        WorkspaceRepository repository = new WorkspaceRepository(jdbc);

        ApiException exception = assertThrows(ApiException.class,
                () -> repository.updateTaskProgress(12L, 7L, 22L, "completed"));

        assertEquals("NODE_LOCKED", exception.code());
        verify(jdbc, org.mockito.Mockito.never()).update(contains("UPDATE task_progress"),
                ArgumentMatchers.<Object[]>any());
    }

    @Test
    void taskOutsideTheStudentsRunIsNotFoundAndNeverUpdated() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(contains("FROM experiment_runs r JOIN coding_workspaces"),
                ArgumentMatchers.<RowMapper<RunRecord>>any(), eq(12L), eq(7L))).thenReturn(List.of(runRecord()));
        when(jdbc.query(contains("FROM node_tasks nt"), ArgumentMatchers.<RowMapper<NodeProgressView>>any(),
                eq(12L), eq(99L), eq(5L))).thenReturn(List.of());
        WorkspaceRepository repository = new WorkspaceRepository(jdbc);

        ApiException exception = assertThrows(ApiException.class,
                () -> repository.updateTaskProgress(12L, 7L, 99L, "completed"));

        assertEquals("TASK_PROGRESS_NOT_FOUND", exception.code());
        verify(jdbc, org.mockito.Mockito.never()).update(contains("UPDATE task_progress"),
                ArgumentMatchers.<Object[]>any());
    }

    private RunRecord runRecord() {
        WorkspaceRecord workspace = new WorkspaceRecord(9L, 12L, 5L, 7L, "unused", "test-image",
                "container", "running", java.math.BigDecimal.ONE, 512);
        return new RunRecord(12L, 5L, 7L, "in_progress", 31L, workspace);
    }
}
