package com.bmhs.patch;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PatchController {
    private final PatchService service;

    public PatchController(PatchService service) {
        this.service = service;
    }

    @PostMapping("/runs/{runId:\\d+}/patches/{patchId:\\d+}/apply")
    public ResponseEntity<PatchModels.PatchApplyView> apply(@PathVariable long runId,
                                                             @PathVariable long patchId,
                                                             AuthenticatedUser user) {
        return ResponseEntity.ok(service.apply(user, runId, patchId));
    }

    @GetMapping("/runs/{runId:\\d+}/patches/{patchId:\\d+}")
    public ResponseEntity<PatchModels.PatchView> get(@PathVariable long runId,
                                                      @PathVariable long patchId,
                                                      AuthenticatedUser user) {
        return ResponseEntity.ok(service.get(user, runId, patchId));
    }
}
