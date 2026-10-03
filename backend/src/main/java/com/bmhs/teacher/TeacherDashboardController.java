package com.bmhs.teacher;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
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
public class TeacherDashboardController {
    private final TeacherService service;

    public TeacherDashboardController(TeacherService service) {
        this.service = service;
    }

    @GetMapping("/courses/{courseId:\\d+}/dashboard")
    public ResponseEntity<TeacherModels.CourseDashboard> dashboard(@PathVariable long courseId,
                                                                     AuthenticatedUser user) {
        return ResponseEntity.ok(service.dashboard(user, courseId));
    }

    @GetMapping("/runs/{runId:\\d+}/timeline")
    public ResponseEntity<?> timeline(@PathVariable long runId, AuthenticatedUser user) {
        return ResponseEntity.ok(service.timeline(user, runId));
    }

    @GetMapping("/runs/{runId:\\d+}/summary")
    public ResponseEntity<TeacherModels.RunSummary> summary(@PathVariable long runId, AuthenticatedUser user) {
        return ResponseEntity.ok(service.summary(user, runId));
    }

    @PostMapping("/runs/{runId:\\d+}/reviews")
    public ResponseEntity<TeacherModels.ReviewView> review(@PathVariable long runId, AuthenticatedUser user,
                                                            @Valid @RequestBody TeacherModels.ReviewRequest request) {
        return ResponseEntity.ok(service.review(user, runId, request));
    }
}
