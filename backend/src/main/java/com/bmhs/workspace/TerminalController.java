package com.bmhs.workspace;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.workspace.TerminalModels.CommandRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TerminalController {
    private final TerminalService service;

    public TerminalController(TerminalService service) {
        this.service = service;
    }

    @PostMapping("/workspaces/{workspaceId:\\d+}/terminal-sessions")
    public ResponseEntity<?> createSession(@PathVariable long workspaceId,
                                           @RequestHeader(value = "Idempotency-Key", required = false) String clientSessionId,
                                           AuthenticatedUser user) {
        return ResponseEntity.ok(service.createSession(user, workspaceId, clientSessionId));
    }

    @PostMapping("/terminal-sessions/{sessionId:\\d+}/commands")
    public ResponseEntity<?> command(@PathVariable long sessionId, AuthenticatedUser user,
                                     @RequestHeader(value = "Idempotency-Key", required = false) String clientCommandId,
                                     @Valid @RequestBody CommandRequest request) {
        return ResponseEntity.ok(service.execute(user, sessionId, clientCommandId, request));
    }

    @GetMapping("/terminal-sessions/{sessionId:\\d+}/commands/{clientCommandId}")
    public ResponseEntity<?> reconcile(@PathVariable long sessionId, @PathVariable String clientCommandId,
                                       AuthenticatedUser user) {
        return ResponseEntity.ok(service.reconcile(user, sessionId, clientCommandId));
    }
}
