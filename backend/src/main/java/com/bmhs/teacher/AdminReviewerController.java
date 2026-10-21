package com.bmhs.teacher;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminReviewerController {
    private final ExperimentReviewerService service;

    public AdminReviewerController(ExperimentReviewerService service) {
        this.service = service;
    }

    @GetMapping("/reviewable-experiments")
    public ResponseEntity<?> experiments(AuthenticatedUser user) {
        return ResponseEntity.ok(service.reviewableExperiments(user));
    }

    @GetMapping("/teachers")
    public ResponseEntity<?> teachers(AuthenticatedUser user) {
        return ResponseEntity.ok(service.activeTeachers(user));
    }
}
