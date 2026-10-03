package com.bmhs.experimentcreation;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.experimentcreation.CreationModels.ConfigurationRequest;
import com.bmhs.experimentcreation.CreationModels.DirectionMessageRequest;
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
@RequestMapping("/api/experiment-creation")
public class ExperimentCreationController {
    private final ExperimentCreationService service;

    public ExperimentCreationController(ExperimentCreationService service) {
        this.service = service;
    }

    @PostMapping("/sessions")
    public ResponseEntity<?> create(AuthenticatedUser user) {
        return ResponseEntity.ok(service.createSession(user.id()));
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<?> get(@PathVariable String sessionId, AuthenticatedUser user) {
        return ResponseEntity.ok(service.get(sessionId, user.id()));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<?> message(@PathVariable String sessionId,
                                     AuthenticatedUser user,
                                     @Valid @RequestBody DirectionMessageRequest request) {
        return ResponseEntity.ok(service.sendDirectionMessage(sessionId, user.id(), request.message()));
    }

    @PostMapping("/sessions/{sessionId}/direction/confirm")
    public ResponseEntity<?> confirmDirection(@PathVariable String sessionId,
                                              AuthenticatedUser user) {
        return ResponseEntity.ok(service.confirmDirection(sessionId, user.id()));
    }

    @PatchMapping("/sessions/{sessionId}/configuration")
    public ResponseEntity<?> configure(@PathVariable String sessionId,
                                       AuthenticatedUser user,
                                       @Valid @RequestBody ConfigurationRequest request) {
        return ResponseEntity.ok(service.configure(sessionId, user.id(), request));
    }

    @PostMapping("/sessions/{sessionId}/plan")
    public ResponseEntity<?> plan(@PathVariable String sessionId,
                                  AuthenticatedUser user) {
        return ResponseEntity.ok(service.generatePlan(sessionId, user.id()));
    }

    @PostMapping("/sessions/{sessionId}/confirm")
    public ResponseEntity<?> materialize(@PathVariable String sessionId,
                                         AuthenticatedUser user,
                                         @org.springframework.web.bind.annotation.RequestHeader("Idempotency-Key") String idempotencyKey) {
        return ResponseEntity.ok(service.materialize(sessionId, user.id(), idempotencyKey));
    }
}
