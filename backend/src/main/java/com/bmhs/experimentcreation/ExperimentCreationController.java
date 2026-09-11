package com.bmhs.experimentcreation;

import com.bmhs.experimentcreation.CreationModels.ConfigurationRequest;
import com.bmhs.experimentcreation.CreationModels.DirectionMessageRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/experiment-creation")
public class ExperimentCreationController {
    private final ExperimentCreationService service;
    private final long demoUserId;
    private final boolean allowDemoUserHeader;

    public ExperimentCreationController(ExperimentCreationService service,
                                        @Value("${bm-hs.demo-user-id:1}") long demoUserId,
                                        @Value("${bm-hs.allow-demo-user-header:false}") boolean allowDemoUserHeader) {
        this.service = service;
        this.demoUserId = demoUserId;
        this.allowDemoUserHeader = allowDemoUserHeader;
    }

    @PostMapping("/sessions")
    public ResponseEntity<?> create(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(service.createSession(user(userId)));
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<?> get(@PathVariable String sessionId,
                                 @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(service.get(sessionId, user(userId)));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<?> message(@PathVariable String sessionId,
                                     @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                     @Valid @RequestBody DirectionMessageRequest request) {
        return ResponseEntity.ok(service.sendDirectionMessage(sessionId, user(userId), request.message()));
    }

    @PostMapping("/sessions/{sessionId}/direction/confirm")
    public ResponseEntity<?> confirmDirection(@PathVariable String sessionId,
                                              @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(service.confirmDirection(sessionId, user(userId)));
    }

    @PatchMapping("/sessions/{sessionId}/configuration")
    public ResponseEntity<?> configure(@PathVariable String sessionId,
                                       @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                       @Valid @RequestBody ConfigurationRequest request) {
        return ResponseEntity.ok(service.configure(sessionId, user(userId), request));
    }

    @PostMapping("/sessions/{sessionId}/plan")
    public ResponseEntity<?> plan(@PathVariable String sessionId,
                                  @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(service.generatePlan(sessionId, user(userId)));
    }

    @PostMapping("/sessions/{sessionId}/confirm")
    public ResponseEntity<?> materialize(@PathVariable String sessionId,
                                         @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                         @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return ResponseEntity.ok(service.materialize(sessionId, user(userId), idempotencyKey));
    }

    private long user(Long userId) {
        if (userId != null && !allowDemoUserHeader) {
            throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "DEMO_USER_HEADER_DISABLED", "当前环境不允许客户端指定用户身份");
        }
        long resolved = userId == null ? demoUserId : userId;
        if (resolved <= 0) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "INVALID_USER_ID", "用户标识必须是正整数");
        }
        return resolved;
    }
}
