package com.bmhs.teacher;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/experiments/{experimentId:\\d+}/reviewer")
public class ExperimentReviewerController {
    private final ExperimentReviewerService service;

    public ExperimentReviewerController(ExperimentReviewerService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ExperimentReviewerModels.AssignmentView> get(@PathVariable long experimentId,
                                                                        AuthenticatedUser user) {
        return ResponseEntity.ok(service.get(user, experimentId));
    }

    @PutMapping
    public ResponseEntity<ExperimentReviewerModels.AssignmentView> assign(@PathVariable long experimentId,
                                                                            AuthenticatedUser user,
                                                                            @Valid @RequestBody ExperimentReviewerModels.AssignmentRequest request) {
        return ResponseEntity.ok(service.assign(user, experimentId, request));
    }
}
