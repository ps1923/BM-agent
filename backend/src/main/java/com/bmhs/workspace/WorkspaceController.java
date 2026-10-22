package com.bmhs.workspace;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.experimentcreation.LegacyWriteDisabled;
import com.bmhs.workspace.WorkspaceModels.RunView;
import com.bmhs.workspace.WorkspaceModels.RunProgressView;
import com.bmhs.workspace.WorkspaceModels.UpdateTaskRequest;
import com.bmhs.workspace.WorkspaceModels.SnapshotRequest;
import com.bmhs.workspace.WorkspaceModels.SnapshotView;
import com.bmhs.workspace.WorkspaceModels.UpdateNodeRequest;
import com.bmhs.workspace.WorkspaceModels.WriteFileRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class WorkspaceController {
    private final WorkspaceService service;

    public WorkspaceController(WorkspaceService service) {
        this.service = service;
    }

    @PostMapping("/experiments/{experimentId:\\d+}/runs")
    public ResponseEntity<RunView> createRun(@PathVariable long experimentId, AuthenticatedUser user) {
        throw LegacyWriteDisabled.exception();
    }

    @PostMapping("/desktop/experiments/{experimentId:\\d+}/runs")
    public ResponseEntity<RunView> createDesktopRun(@PathVariable long experimentId, AuthenticatedUser user) {
        throw LegacyWriteDisabled.exception();
    }

    @GetMapping("/runs/{runId:\\d+}")
    public ResponseEntity<RunView> getRun(@PathVariable long runId, AuthenticatedUser user) {
        return ResponseEntity.ok(service.getRun(user, runId));
    }

    @GetMapping("/runs/{runId:\\d+}/progress")
    public ResponseEntity<RunProgressView> getProgress(@PathVariable long runId, AuthenticatedUser user) {
        return ResponseEntity.ok(service.getProgress(user, runId));
    }

    @PatchMapping("/runs/{runId:\\d+}/nodes/{nodeId:\\d+}")
    public ResponseEntity<RunProgressView> updateNode(@PathVariable long runId, @PathVariable long nodeId,
                                                       AuthenticatedUser user,
                                                       @Valid @RequestBody UpdateNodeRequest request) {
        service.assertGitProgressWrite(user, runId);
        return ResponseEntity.ok(service.updateNode(user, runId, nodeId, request));
    }

    @PatchMapping("/runs/{runId:\\d+}/tasks/{taskId:\\d+}")
    public ResponseEntity<RunProgressView> updateTask(@PathVariable long runId, @PathVariable long taskId,
                                                       AuthenticatedUser user,
                                                       @Valid @RequestBody UpdateTaskRequest request) {
        service.assertGitProgressWrite(user, runId);
        return ResponseEntity.ok(service.updateTask(user, runId, taskId, request));
    }

    @GetMapping("/workspaces/{workspaceId:\\d+}/files")
    public ResponseEntity<?> listFiles(@PathVariable long workspaceId, AuthenticatedUser user) {
        return ResponseEntity.ok(service.listFiles(user, workspaceId));
    }

    @GetMapping("/workspaces/{workspaceId:\\d+}/files/{*path}")
    public ResponseEntity<?> readFile(@PathVariable long workspaceId, @PathVariable String path,
                                      AuthenticatedUser user) {
        return ResponseEntity.ok(service.readFile(user, workspaceId, requestPath(path)));
    }

    @PutMapping("/workspaces/{workspaceId:\\d+}/files/{*path}")
    public ResponseEntity<?> writeFile(@PathVariable long workspaceId, @PathVariable String path,
                                       AuthenticatedUser user, @Valid @RequestBody WriteFileRequest request) {
        throw LegacyWriteDisabled.exception();
    }

    @PostMapping("/workspaces/{workspaceId:\\d+}/snapshots")
    public ResponseEntity<SnapshotView> createSnapshot(@PathVariable long workspaceId, AuthenticatedUser user,
                                                        @Valid @RequestBody(required = false) SnapshotRequest request) {
        throw LegacyWriteDisabled.exception();
    }

    private String requestPath(String path) {
        // Spring's {*path} wildcard includes one routing separator in the value.
        // Remove only that framework-added separator; WorkspaceService keeps the
        // absolute-path and traversal checks for all other input.
        return path != null && path.startsWith("/") ? path.substring(1) : path;
    }
}
