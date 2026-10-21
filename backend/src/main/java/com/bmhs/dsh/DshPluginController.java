package com.bmhs.dsh;

import com.bmhs.rag.RagModels;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plugin")
public class DshPluginController {
    private final DshPluginService service;

    public DshPluginController(DshPluginService service) {
        this.service = service;
    }

    @GetMapping("/experiments")
    public ResponseEntity<?> experiments(DshPrincipal principal) {
        return ResponseEntity.ok(service.experiments(principal));
    }

    @PostMapping("/runs")
    public ResponseEntity<DshPluginModels.RunView> bindRun(DshPrincipal principal,
                                                            @Valid @RequestBody DshPluginModels.RunBindRequest request) {
        return ResponseEntity.ok(service.bindRun(principal, request));
    }

    @GetMapping("/runs/{runId:\\d+}/learning-path")
    public ResponseEntity<?> learningPath(DshPrincipal principal, @PathVariable long runId) {
        return ResponseEntity.ok(service.learningPath(principal, runId));
    }

    @PatchMapping("/runs/{runId:\\d+}/nodes/{nodeId:\\d+}")
    public ResponseEntity<?> updateNode(DshPrincipal principal, @PathVariable long runId,
                                        @PathVariable long nodeId,
                                        @Valid @RequestBody DshPluginModels.ProgressUpdateRequest request) {
        service.updateNode(principal, runId, nodeId, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/runs/{runId:\\d+}/tasks/{taskId:\\d+}")
    public ResponseEntity<?> updateTask(DshPrincipal principal, @PathVariable long runId,
                                        @PathVariable long taskId,
                                        @Valid @RequestBody DshPluginModels.ProgressUpdateRequest request) {
        service.updateTask(principal, runId, taskId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/experiments/{experimentId:\\d+}/bug-memory/search")
    public ResponseEntity<?> searchMemory(DshPrincipal principal, @PathVariable long experimentId,
                                          @Valid @RequestBody DshPluginService.MemorySearchRequest request) {
        return ResponseEntity.ok(service.searchMemory(principal, experimentId, request));
    }

    @PostMapping("/runs/{runId:\\d+}/bugs")
    public ResponseEntity<?> submitBug(DshPrincipal principal, @PathVariable long runId,
                                       @Valid @RequestBody RagModels.BugRequest request) {
        return ResponseEntity.ok(service.submitBug(principal, runId, request));
    }

    @PostMapping("/runs/{runId:\\d+}/code-submissions")
    public ResponseEntity<?> submitCode(DshPrincipal principal, @PathVariable long runId,
                                        @Valid @RequestBody DshPluginModels.CodeSubmissionRequest request) {
        return ResponseEntity.ok(service.submitCode(principal, runId, request));
    }
}
