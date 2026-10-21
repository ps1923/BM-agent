package com.bmhs.workspace;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.workspace.WorkspaceModels.RunRecord;
import com.bmhs.workspace.WorkspaceModels.WorkspaceRecord;
import com.bmhs.workspace.WorkspaceModels.RunView;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DesktopWorkspaceServiceTest {
    @Test
    void createsDesktopBindingWithoutStartingServerRuntime() {
        WorkspaceRepository repository = mock(WorkspaceRepository.class);
        WorkspaceRuntime runtime = mock(WorkspaceRuntime.class);
        WorkspaceService service = new WorkspaceService(repository, runtime,
                "unused-server-root", "unused-image", BigDecimal.ONE, 1024, 2_000_000, 20_000_000);
        String opaqueId = "9e7ea9bc-e4f4-4ed7-a0c5-1e5a4e6c025a";
        WorkspaceRecord workspace = new WorkspaceRecord(8L, 7L, 4L, 3L,
                "desktop://" + opaqueId, "desktop-local", null, "running", null, 0,
                "desktop", opaqueId);
        when(repository.createOrFindDesktopRun(4L, 3L))
                .thenReturn(new RunRecord(7L, 4L, 3L, "not_started", null, workspace));
        when(repository.toView(new RunRecord(7L, 4L, 3L, "not_started", null, workspace)))
                .thenReturn(new RunView(7L, 4L, "not_started", null, 8L, "running", "desktop", opaqueId));

        var result = service.createDesktopRun(new AuthenticatedUser(3L, "s@example.edu", "Student", "student"), 4L);

        assertEquals("desktop", result.workspaceType());
        assertEquals(opaqueId, result.localWorkspaceId());
        assertEquals("running", result.workspaceStatus());
        verify(runtime, never()).start(workspace);
        verify(repository).createOrFindDesktopRun(4L, 3L);
    }
}
