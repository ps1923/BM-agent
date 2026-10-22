package com.bmhs.git;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.experimentcreation.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class GitAcceptanceController {
    private final GitAcceptanceService service;

    public GitAcceptanceController(GitAcceptanceService service) { this.service = service; }

    @PostMapping("/api/student/experiments/{experimentId:\\d+}/runs")
    public ResponseEntity<GitModels.RunView> createRun(@PathVariable long experimentId, AuthenticatedUser user) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.createRun(user, experimentId));
    }

    @GetMapping("/api/student/runs/{runId:\\d+}")
    public ResponseEntity<GitModels.RunView> getRun(@PathVariable long runId, AuthenticatedUser user) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.getRun(user, runId));
    }

    @GetMapping("/api/student/runs/{runId:\\d+}/repository")
    public ResponseEntity<GitModels.RepositoryView> repository(@PathVariable long runId, AuthenticatedUser user) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.repository(user, runId));
    }

    @PostMapping("/api/student/runs/{runId:\\d+}/repository-credentials")
    public ResponseEntity<GitModels.CredentialsView> credentials(@PathVariable long runId, AuthenticatedUser user) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header(HttpHeaders.PRAGMA, "no-cache")
                .body(service.credentials(user, runId));
    }

    @PostMapping("/api/student/runs/{runId:\\d+}/submissions")
    public ResponseEntity<GitModels.SubmitResponse> submit(@PathVariable long runId, AuthenticatedUser user,
                                                            @Valid @RequestBody GitModels.SubmitRequest request) {
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(service.submit(user, runId, request));
    }

    @GetMapping("/api/student/runs/{runId:\\d+}/submissions")
    public ResponseEntity<?> studentSubmissions(@PathVariable long runId, AuthenticatedUser user) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.studentSubmissions(user, runId));
    }

    @GetMapping("/api/student/submissions/{submissionId:\\d+}")
    public ResponseEntity<GitModels.StudentSubmissionView> studentSubmission(@PathVariable long submissionId,
                                                                              AuthenticatedUser user) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.studentSubmission(user, submissionId));
    }

    @PostMapping("/api/student/submissions/{submissionId:\\d+}/bug-draft/{nodeId:\\d+}/confirm")
    public ResponseEntity<?> confirmBugDraft(@PathVariable long submissionId, @PathVariable long nodeId,
                                              AuthenticatedUser user,
                                              @Valid @RequestBody GitModels.BugDraftConfirmRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.confirmBugDraft(user, submissionId, nodeId, request));
    }

    @GetMapping("/api/teacher/runs/{runId:\\d+}/submissions")
    public ResponseEntity<?> teacherSubmissions(@PathVariable long runId, AuthenticatedUser user) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.teacherSubmissions(user, runId));
    }

    @GetMapping("/api/teacher/submissions/{submissionId:\\d+}")
    public ResponseEntity<GitModels.TeacherSubmissionDetail> teacherSubmission(@PathVariable long submissionId,
                                                                                AuthenticatedUser user) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.teacherSubmission(user, submissionId));
    }

    @PostMapping("/api/integrations/gitea/webhook")
    public ResponseEntity<GitModels.WebhookAck> webhook(
            @RequestHeader(name = "X-Gitea-Event", required = false) String event,
            @RequestHeader(name = "X-Gitea-Signature", required = false) String signature,
            HttpServletRequest request) {
        byte[] payload;
        try {
            payload = request.getInputStream().readNBytes(2_000_001);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "GITEA_WEBHOOK_INVALID", "Gitea webhook 内容无法读取");
        }
        if (payload.length > 2_000_000) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "GITEA_WEBHOOK_TOO_LARGE", "Gitea webhook 内容超过大小上限");
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.webhook(event, signature, payload));
    }
}
