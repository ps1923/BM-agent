package com.bmhs.guidance;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class GuidanceController {
    private final GuidanceService service;

    public GuidanceController(GuidanceService service) {
        this.service = service;
    }

    @PostMapping("/runs/{runId:\\d+}/guidance")
    public ResponseEntity<GuidanceModels.GuidanceResponse> guidance(
            @PathVariable long runId, AuthenticatedUser user,
            @Valid @RequestBody GuidanceModels.GuidanceRequest request) {
        return ResponseEntity.ok(service.guide(user, runId, request));
    }
}
