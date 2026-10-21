package com.bmhs.workspace;

import com.bmhs.auth.AuthService;
import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.auth.CurrentUserArgumentResolver;
import com.bmhs.auth.SessionAuthenticationFilter;
import com.bmhs.experimentcreation.ApiException;
import com.bmhs.experimentcreation.ApiExceptionHandler;
import com.bmhs.workspace.WorkspaceModels.NodeProgressView;
import com.bmhs.workspace.WorkspaceModels.RunProgressView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import jakarta.servlet.http.Cookie;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class WorkspaceControllerWebTest {
    private final AuthenticatedUser student = new AuthenticatedUser(7L, "student@example.test", "Student", "student");
    private final WorkspaceRepository repository = mock(WorkspaceRepository.class);
    private final WorkspaceRuntime runtime = mock(WorkspaceRuntime.class);
    private final AuthService authService = mock(AuthService.class);
    private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        validator.afterPropertiesSet();
        when(authService.authenticate("test-session")).thenReturn(student);
        WorkspaceService service = new WorkspaceService(repository, runtime, "unused-root", "test-image",
                BigDecimal.ONE, 512, 100, 1000);
        mvc = standaloneSetup(new WorkspaceController(service))
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .addFilters(new SessionAuthenticationFilter(authService, "BM_SESSION"))
                .build();
    }

    @AfterEach
    void closeValidator() {
        validator.close();
    }

    @Test
    void taskPatchUsesAuthenticatedStudentAndReturnsPersistedProgress() throws Exception {
        RunProgressView progress = new RunProgressView(8L, 5L, "in_progress", 12L, List.of(),
                List.of(new WorkspaceModels.TaskProgressView(22L, 12L, "completed", null)));
        when(repository.updateTaskProgress(8L, student.id(), 22L, "completed")).thenReturn(progress);

        mvc.perform(patch("/api/runs/8/tasks/22")
                        .cookie(new Cookie("BM_SESSION", "test-session"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"completed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(8))
                .andExpect(jsonPath("$.tasks[0].taskId").value(22))
                .andExpect(jsonPath("$.tasks[0].status").value("completed"));

        verify(repository).updateTaskProgress(8L, student.id(), 22L, "completed");
    }

    @Test
    void directRestNodeCompletionReturnsStableConflictWhenTasksAreIncomplete() throws Exception {
        WorkspaceModels.WorkspaceRecord workspace = new WorkspaceModels.WorkspaceRecord(9L, 8L, 5L,
                student.id(), "unused-root/run-8", "test-image", "container", "running", BigDecimal.ONE, 512);
        when(repository.findRun(8L, student.id())).thenReturn(new WorkspaceModels.RunRecord(
                8L, 5L, student.id(), "in_progress", 12L, workspace));
        RunProgressView before = new RunProgressView(8L, 5L, "in_progress", 12L,
                List.of(new NodeProgressView(12L, "in_progress", BigDecimal.valueOf(50), null, null)), List.of());
        when(repository.findProgress(8L, student.id())).thenReturn(before);
        when(repository.updateNodeProgress(8L, student.id(), 12L, "completed", BigDecimal.valueOf(100)))
                .thenThrow(new ApiException(HttpStatus.CONFLICT, "NODE_TASKS_INCOMPLETE", "请先完成当前节点的所有任务"));

        mvc.perform(patch("/api/runs/8/nodes/12")
                        .cookie(new Cookie("BM_SESSION", "test-session"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"completed\",\"progressPercent\":100}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NODE_TASKS_INCOMPLETE"));

        verify(repository).updateNodeProgress(8L, student.id(), 12L, "completed", BigDecimal.valueOf(100));
        verify(repository, never()).createNodeCompletionSnapshot(anyLong(), anyLong(), anyLong(), anyString());
    }

    @Test
    void invalidTaskStatusIsRejectedByHttpValidationBeforeServiceMutation() throws Exception {
        mvc.perform(patch("/api/runs/8/tasks/22")
                        .cookie(new Cookie("BM_SESSION", "test-session"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"done\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verify(repository, never()).updateTaskProgress(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    void invalidNodeProgressIsRejectedOverHttpBeforeServiceMutation() throws Exception {
        mvc.perform(patch("/api/runs/8/nodes/12")
                        .cookie(new Cookie("BM_SESSION", "test-session"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"in_progress\",\"progressPercent\":101}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verify(repository, never()).updateNodeProgress(anyLong(), anyLong(), anyLong(), any(), any());
    }

    @Test
    void taskEndpointRejectsMissingAuthentication() throws Exception {
        mvc.perform(patch("/api/runs/8/tasks/22")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"completed\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        verify(repository, never()).updateTaskProgress(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    void unknownSessionCookieIsRejectedBeforeRepositoryAccess() throws Exception {
        when(authService.authenticate("expired-or-forged")).thenReturn(null);

        mvc.perform(patch("/api/runs/8/tasks/22")
                        .cookie(new Cookie("BM_SESSION", "expired-or-forged"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"completed\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        verify(authService).authenticate("expired-or-forged");
        verifyNoInteractions(repository);
    }

    @Test
    void teacherCannotPatchStudentNodeOrTaskProgressBeforeRepositoryAccess() throws Exception {
        AuthenticatedUser teacher = new AuthenticatedUser(3L, "teacher@example.test", "Teacher", "teacher");
        when(authService.authenticate("teacher-session")).thenReturn(teacher);

        mvc.perform(patch("/api/runs/8/nodes/12")
                        .cookie(new Cookie("BM_SESSION", "teacher-session"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"completed\",\"progressPercent\":100}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROLE_FORBIDDEN"));

        mvc.perform(patch("/api/runs/8/tasks/22")
                        .cookie(new Cookie("BM_SESSION", "teacher-session"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"completed\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROLE_FORBIDDEN"));

        verifyNoInteractions(repository);
    }
}
