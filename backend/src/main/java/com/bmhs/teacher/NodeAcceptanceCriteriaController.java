package com.bmhs.teacher;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class NodeAcceptanceCriteriaController {
    private final NodeAcceptanceCriteriaService service;

    public NodeAcceptanceCriteriaController(NodeAcceptanceCriteriaService service) { this.service = service; }

    @GetMapping("/api/teacher/experiments/{experimentId:\\d+}/acceptance-criteria")
    public ResponseEntity<List<NodeAcceptanceCriteriaModels.NodeView>> list(
            @PathVariable long experimentId, AuthenticatedUser user) {
        return ResponseEntity.ok(service.list(user, experimentId));
    }

    @GetMapping("/api/teacher/experiments/{experimentId:\\d+}/nodes/{nodeId:\\d+}/acceptance-criteria/history")
    public ResponseEntity<List<NodeAcceptanceCriteriaModels.RevisionView>> history(
            @PathVariable long experimentId, @PathVariable long nodeId, AuthenticatedUser user) {
        return ResponseEntity.ok(service.history(user, experimentId, nodeId));
    }

    @PutMapping("/api/teacher/experiments/{experimentId:\\d+}/nodes/{nodeId:\\d+}/acceptance-criteria")
    public ResponseEntity<NodeAcceptanceCriteriaModels.RevisionView> update(
            @PathVariable long experimentId, @PathVariable long nodeId, AuthenticatedUser user,
            @Valid @RequestBody NodeAcceptanceCriteriaModels.UpdateRequest request) {
        return ResponseEntity.ok(service.update(user, experimentId, nodeId, request));
    }
}
