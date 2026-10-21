package com.bmhs.workspace;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.auth.AuthService;
import com.bmhs.auth.CurrentUserArgumentResolver;
import com.bmhs.auth.SessionAuthenticationFilter;
import com.bmhs.experimentcreation.ApiExceptionHandler;
import com.bmhs.workspace.TerminalModels.CommandRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import jakarta.servlet.http.Cookie;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class TerminalControllerWebTest {
    private static final String COMMAND_ID = "12345678-1234-4234-8234-123456789abc";
    private final AuthenticatedUser student = new AuthenticatedUser(7L, "student@example.test", "Student", "student");
    private final AuthenticatedUser teacher = new AuthenticatedUser(9L, "teacher@example.test", "Teacher", "teacher");
    private final TerminalRepository repository = mock(TerminalRepository.class);
    private final WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);
    private final TerminalRuntime runtime = mock(TerminalRuntime.class);
    private final AuthService authService = mock(AuthService.class);
    private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        validator.afterPropertiesSet();
        when(authService.authenticate("student-session")).thenReturn(student);
        when(authService.authenticate("teacher-session")).thenReturn(teacher);
        TerminalService service = new TerminalService(repository, workspaceRepository, runtime,
                Duration.ofSeconds(2), 4096);
        mvc = standaloneSetup(new TerminalController(service))
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
    void missingIdempotencyKeyFailsBeforeAnyCommandLookupOrExecution() throws Exception {
        mvc.perform(post("/api/terminal-sessions/22/commands")
                        .cookie(new Cookie("BM_SESSION", "student-session"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"commandText\":\"pwd\",\"workingDirectory\":\".\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IDEMPOTENCY_KEY"));

        verifyNoInteractions(repository, workspaceRepository, runtime);
    }

    @Test
    void terminalSessionCreationRequiresAnIdempotencyKey() throws Exception {
        mvc.perform(post("/api/workspaces/73/terminal-sessions")
                        .cookie(new Cookie("BM_SESSION", "student-session")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IDEMPOTENCY_KEY"));

        verifyNoInteractions(repository, workspaceRepository, runtime);
    }

    @Test
    void teacherCannotReadStudentCommandStatus() throws Exception {
        mvc.perform(get("/api/terminal-sessions/22/commands/{id}", COMMAND_ID)
                        .cookie(new Cookie("BM_SESSION", "teacher-session")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROLE_FORBIDDEN"));

        verifyNoInteractions(repository, workspaceRepository, runtime);
    }

    @Test
    void authenticatedStudentCanReconcileOwnPendingCommandThroughHttpRoute() throws Exception {
        when(repository.findCommand(22L, COMMAND_ID, student.id())).thenReturn(new CommandRecord(
                48L, 22L, "pwd", "/workspace", "running", null,
                null, Path.of("unused").toAbsolutePath().toString(), Instant.now()));

        mvc.perform(get("/api/terminal-sessions/22/commands/{id}", COMMAND_ID)
                        .cookie(new Cookie("BM_SESSION", "student-session")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("running"))
                .andExpect(jsonPath("$.output").value(""));

        verify(repository).findCommand(22L, COMMAND_ID, student.id());
        verifyNoInteractions(runtime);
    }
}
