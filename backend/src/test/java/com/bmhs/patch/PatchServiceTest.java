package com.bmhs.patch;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.experimentcreation.ApiException;
import com.bmhs.workspace.WorkspaceModels;
import com.bmhs.workspace.WorkspaceService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PatchServiceTest {
    private final AuthenticatedUser student = new AuthenticatedUser(7L, "student@example.com", "学生", "student");

    @Test
    void teacherCannotApplyPatch() {
        PatchService service = new PatchService(mock(PatchRepository.class), mock(WorkspaceService.class));
        AuthenticatedUser teacher = new AuthenticatedUser(3L, "teacher@example.com", "教师", "teacher");

        ApiException exception = assertThrows(ApiException.class, () -> service.apply(teacher, 3L, 4L));

        assertEquals("ROLE_FORBIDDEN", exception.code());
    }

    @Test
    void teacherCannotReadPatchDetails() {
        PatchService service = new PatchService(mock(PatchRepository.class), mock(WorkspaceService.class));
        AuthenticatedUser teacher = new AuthenticatedUser(3L, "teacher@example.com", "教师", "teacher");

        ApiException exception = assertThrows(ApiException.class, () -> service.get(teacher, 3L, 4L));

        assertEquals("ROLE_FORBIDDEN", exception.code());
    }

    @Test
    void studentCanReadPatchDetailsThroughRunOwnershipRepositoryQuery() {
        PatchRepository repository = mock(PatchRepository.class);
        PatchModels.PatchFile file = new PatchModels.PatchFile("src/Main.java", "hash", "class Main {}\n");
        PatchModels.PatchView expected = new PatchModels.PatchView(4L, 3L, 9L, "修复", "pending",
                List.of(file), Instant.now(), null, null);
        when(repository.findForStudent(4L, 3L, 7L)).thenReturn(expected);

        PatchModels.PatchView result = new PatchService(repository, mock(WorkspaceService.class)).get(student, 3L, 4L);

        assertEquals(expected, result);
        verify(repository).findForStudent(4L, 3L, 7L);
    }

    @Test
    void alreadyAppliedPatchIsIdempotentAndDoesNotRewriteFiles() {
        PatchRepository repository = mock(PatchRepository.class);
        WorkspaceService workspace = mock(WorkspaceService.class);
        PatchModels.PatchFile file = new PatchModels.PatchFile("src/Main.java", "hash", "class Main {}");
        when(repository.findForStudent(4L, 3L, 7L)).thenReturn(new PatchModels.PatchView(4L, 3L, 9L,
                "修复", "applied", List.of(file), Instant.now(), Instant.now(), 12L));

        PatchModels.PatchApplyView result = new PatchService(repository, workspace).apply(student, 3L, 4L);

        assertEquals(12L, result.snapshotId());
        verify(workspace, org.mockito.Mockito.never()).applyPatch(org.mockito.Mockito.any(), org.mockito.Mockito.anyLong(), org.mockito.Mockito.any());
    }

    @Test
    void pendingPatchCreatesSnapshotThenMarksApplied() {
        PatchRepository repository = mock(PatchRepository.class);
        WorkspaceService workspace = mock(WorkspaceService.class);
        PatchModels.PatchFile file = new PatchModels.PatchFile("src/Main.java", "hash", "class Main {}");
        when(repository.findForStudent(4L, 3L, 7L)).thenReturn(new PatchModels.PatchView(4L, 3L, 9L,
                "修复", "pending", List.of(file), Instant.now(), null, null));
        when(workspace.createSnapshot(student, 9L, new WorkspaceModels.SnapshotRequest("应用补丁前自动快照")))
                .thenReturn(new WorkspaceModels.SnapshotView(12L, 9L, ".snapshots/12", "manual", "应用补丁前自动快照", Instant.now()));

        PatchModels.PatchApplyView result = new PatchService(repository, workspace).apply(student, 3L, 4L);

        assertEquals("applied", result.status());
        assertEquals(12L, result.snapshotId());
        verify(workspace).applyPatch(student, 9L, List.of(file));
        verify(repository).markApplied(4L, 3L, 7L, 12L);
    }
}
