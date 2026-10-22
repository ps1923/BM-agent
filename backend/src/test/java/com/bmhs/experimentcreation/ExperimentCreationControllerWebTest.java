package com.bmhs.experimentcreation;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.auth.AuthService;
import com.bmhs.auth.CurrentUserArgumentResolver;
import com.bmhs.auth.SessionAuthenticationFilter;
import com.bmhs.experimentcreation.CreationModels.ConversationMessage;
import com.bmhs.experimentcreation.CreationModels.SessionDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.servlet.http.Cookie;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ExperimentCreationControllerWebTest {
    private final AuthenticatedUser student = new AuthenticatedUser(7L, "student@example.test", "Student", "student");
    private final ExperimentCreationService service = mock(ExperimentCreationService.class);
    private final AuthService authService = mock(AuthService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        when(authService.authenticate("test-session")).thenReturn(student);
        mvc = standaloneSetup(new ExperimentCreationController(service))
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                .setControllerAdvice(new ApiExceptionHandler())
                .addFilters(new SessionAuthenticationFilter(authService, "BM_SESSION"))
                .build();
    }

    @Test
    void draftListIsBoundToAuthenticatedStudent() throws Exception {
        when(service.listDrafts(student.id())).thenReturn(List.of(new SessionDraft(
                "draft-1", "direction_discussion", "2026-10-22T10:00:00.000000",
                "学习 Django", null, null)));

        mvc.perform(get("/api/experiment-creation/sessions/drafts")
                        .cookie(new Cookie("BM_SESSION", "test-session")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sessionId").value("draft-1"))
                .andExpect(jsonPath("$[0].title").value("学习 Django"));

        verify(service).listDrafts(student.id());
    }

    @Test
    void orderedMessagesEndpointUsesAuthenticatedStudent() throws Exception {
        when(service.getMessages("draft-1", student.id())).thenReturn(List.of(
                new ConversationMessage("user", "学习 Django", "2026-10-22T10:00:00.000000"),
                new ConversationMessage("assistant", "可以从虚拟环境开始。", "2026-10-22T10:00:01.000000")));

        mvc.perform(get("/api/experiment-creation/sessions/draft-1/messages")
                        .cookie(new Cookie("BM_SESSION", "test-session"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("user"))
                .andExpect(jsonPath("$[1].content").value("可以从虚拟环境开始。"));

        verify(service).getMessages("draft-1", student.id());
    }

    @Test
    void draftEndpointsRequireAnAuthenticatedSession() throws Exception {
        mvc.perform(get("/api/experiment-creation/sessions/drafts"))
                .andExpect(status().isUnauthorized());

        org.mockito.Mockito.verifyNoInteractions(service);
    }
}
