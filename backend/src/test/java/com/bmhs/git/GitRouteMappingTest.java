package com.bmhs.git;

import com.bmhs.dsh.DshPluginService;
import com.bmhs.dsh.TeacherDshController;
import com.bmhs.experimentcreation.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GitRouteMappingTest {
    @Test
    void gitAndLegacyDshTeacherRoutesRegisterTogetherWithoutAmbiguity() {
        MockMvcBuilders.standaloneSetup(
                new GitAcceptanceController(mock(GitAcceptanceService.class)),
                new GitAdminSettingsController(mock(GitAdminSettingsService.class)),
                new TeacherDshController(mock(DshPluginService.class)))
                .build();
    }

    @Test
    void oversizedWebhookIsRejectedBeforeSignatureVerification() {
        GitAcceptanceService service = mock(GitAcceptanceService.class);
        GitAcceptanceController controller = new GitAcceptanceController(service);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(new byte[2_000_001]);

        ApiException exception = assertThrows(ApiException.class, () -> controller.webhook("push", "invalid", request));

        assertEquals("GITEA_WEBHOOK_TOO_LARGE", exception.code());
        verify(service, never()).webhook(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }
}
