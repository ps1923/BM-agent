package com.bmhs.dsh;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.experimentcreation.LegacyWriteDisabled;
import com.bmhs.teacher.TeacherModels;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher")
public class TeacherDshController {
    private final DshPluginService service;

    public TeacherDshController(DshPluginService service) {
        this.service = service;
    }

    @GetMapping("/experiments")
    public ResponseEntity<?> experiments(AuthenticatedUser user) {
        return ResponseEntity.ok(service.teacherExperiments(user));
    }

    @GetMapping("/experiments/{experimentId:\\d+}/runs")
    public ResponseEntity<?> runs(@PathVariable long experimentId, AuthenticatedUser user) {
        return ResponseEntity.ok(service.teacherRuns(user, experimentId));
    }

    @GetMapping("/dsh/runs/{runId:\\d+}/submissions")
    public ResponseEntity<?> submissions(@PathVariable long runId, AuthenticatedUser user) {
        return ResponseEntity.ok(service.teacherSubmissions(user, runId));
    }

    @GetMapping("/dsh/runs/{runId:\\d+}/submissions/{submissionId:\\d+}")
    public ResponseEntity<?> submission(@PathVariable long runId, @PathVariable long submissionId,
                                        AuthenticatedUser user) {
        return ResponseEntity.ok(service.teacherSubmission(user, runId, submissionId));
    }

    @GetMapping("/dsh/runs/{runId:\\d+}/review")
    public ResponseEntity<TeacherModels.ReviewView> review(@PathVariable long runId, AuthenticatedUser user) {
        return ResponseEntity.ok(service.teacherDshReview(user, runId));
    }

    @PostMapping("/dsh/runs/{runId:\\d+}/review")
    public ResponseEntity<TeacherModels.ReviewView> saveReview(@PathVariable long runId, AuthenticatedUser user,
                                                               @Valid @RequestBody TeacherModels.ReviewRequest request) {
        throw LegacyWriteDisabled.exception();
    }
}
