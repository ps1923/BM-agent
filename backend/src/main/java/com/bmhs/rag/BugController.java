package com.bmhs.rag;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BugController {
    private final BugService service;

    public BugController(BugService service) {
        this.service = service;
    }

    @PostMapping("/runs/{runId:\\d+}/bugs")
    public ResponseEntity<RagModels.BugCaseView> create(@PathVariable long runId, AuthenticatedUser user,
                                                         @Valid @RequestBody RagModels.BugRequest request) {
        return ResponseEntity.ok(service.create(user, runId, request));
    }

    @GetMapping("/bugs")
    public ResponseEntity<?> list(@RequestParam(required = false) Long courseId, AuthenticatedUser user) {
        return ResponseEntity.ok(service.list(user, courseId));
    }

    @PostMapping("/bugs/{bugCaseId:\\d+}/approve")
    public ResponseEntity<RagModels.BugCaseView> review(@PathVariable long bugCaseId, AuthenticatedUser user,
                                                         @Valid @RequestBody RagModels.BugReviewRequest request) {
        return ResponseEntity.ok(service.review(user, bugCaseId, request));
    }
}
