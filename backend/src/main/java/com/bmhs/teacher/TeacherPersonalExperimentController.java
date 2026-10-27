package com.bmhs.teacher;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher/personal-experiments")
public class TeacherPersonalExperimentController {
    private final ExperimentReviewerService service;

    public TeacherPersonalExperimentController(ExperimentReviewerService service) {
        this.service = service;
    }

    @GetMapping("/reviewable")
    public ResponseEntity<?> experiments(AuthenticatedUser user) {
        return ResponseEntity.ok(service.reviewableExperiments(user));
    }
}
