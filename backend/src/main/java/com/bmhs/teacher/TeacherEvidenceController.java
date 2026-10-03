package com.bmhs.teacher;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher/runs/{runId:\\d+}")
public class TeacherEvidenceController {
    private final TeacherEvidenceService service;

    public TeacherEvidenceController(TeacherEvidenceService service) {
        this.service = service;
    }

    @GetMapping("/stages")
    public ResponseEntity<?> stages(@PathVariable long runId, AuthenticatedUser user) {
        return ResponseEntity.ok(service.stages(user, runId));
    }

    @GetMapping("/snapshots/{snapshotId:\\d+}/files")
    public ResponseEntity<?> files(@PathVariable long runId, @PathVariable long snapshotId, AuthenticatedUser user) {
        return ResponseEntity.ok(service.files(user, runId, snapshotId));
    }

    @GetMapping("/snapshots/{snapshotId:\\d+}/files/{*path}")
    public ResponseEntity<?> readFile(@PathVariable long runId, @PathVariable long snapshotId,
                                      @PathVariable String path, AuthenticatedUser user) {
        String requestPath = path != null && path.startsWith("/") ? path.substring(1) : path;
        return ResponseEntity.ok(service.readFile(user, runId, snapshotId, requestPath));
    }
}
