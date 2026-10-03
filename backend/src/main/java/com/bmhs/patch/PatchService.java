package com.bmhs.patch;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.experimentcreation.ApiException;
import com.bmhs.workspace.WorkspaceModels;
import com.bmhs.workspace.WorkspaceService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PatchService {
    private final PatchRepository repository;
    private final WorkspaceService workspaceService;

    public PatchService(PatchRepository repository, WorkspaceService workspaceService) {
        this.repository = repository;
        this.workspaceService = workspaceService;
    }

    public PatchModels.PatchApplyView apply(AuthenticatedUser user, long runId, long patchId) {
        requireStudent(user);
        PatchModels.PatchView patch = repository.findForStudent(patchId, runId, user.id());
        if ("applied".equals(patch.status())) {
            return new PatchModels.PatchApplyView(patch.id(), patch.status(), patch.snapshotId() == null ? 0L : patch.snapshotId(),
                    patch.files().stream().map(PatchModels.PatchFile::path).toList());
        }
        if (!"pending".equals(patch.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "PATCH_STATE_CONFLICT", "补丁当前状态不允许应用");
        }
        validate(patch.files());
        WorkspaceModels.SnapshotView snapshot = workspaceService.createSnapshot(user, patch.workspaceId(),
                new WorkspaceModels.SnapshotRequest("应用补丁前自动快照"));
        workspaceService.applyPatch(user, patch.workspaceId(), patch.files());
        repository.markApplied(patch.id(), runId, user.id(), snapshot.id());
        return new PatchModels.PatchApplyView(patch.id(), "applied", snapshot.id(),
                patch.files().stream().map(PatchModels.PatchFile::path).toList());
    }

    public PatchModels.PatchView get(AuthenticatedUser user, long runId, long patchId) {
        requireStudent(user);
        return repository.findForStudent(patchId, runId, user.id());
    }

    private void validate(List<PatchModels.PatchFile> files) {
        if (files == null || files.isEmpty() || files.size() > 20) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PATCH_FILES_INVALID", "补丁文件数量无效");
        }
        Set<String> paths = new HashSet<>();
        for (PatchModels.PatchFile file : files) {
            if (file == null || file.path() == null || file.path().isBlank()
                    || file.content() == null || file.expectedHash() == null || file.expectedHash().isBlank()
                    || !paths.add(file.path())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "PATCH_FILES_INVALID", "补丁文件内容或哈希无效");
            }
        }
    }

    private void requireStudent(AuthenticatedUser user) {
        if (!"student".equals(user.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ROLE_FORBIDDEN", "只有学生可以应用代码补丁");
        }
    }
}
