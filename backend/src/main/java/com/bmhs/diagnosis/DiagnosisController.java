package com.bmhs.diagnosis;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class DiagnosisController {
    private final DiagnosisService service;
    public DiagnosisController(DiagnosisService service) { this.service = service; }

    @PostMapping("/runs/{runId:\\d+}/deep-diagnosis")
    public ResponseEntity<DiagnosisModels.DiagnosisView> submit(@PathVariable long runId, AuthenticatedUser user,
            @Valid @RequestBody(required = false) DiagnosisModels.DiagnosisRequest request) {
        return ResponseEntity.accepted().body(service.submit(user, runId, request));
    }

    @GetMapping("/diagnoses/{diagnosisId:\\d+}")
    public ResponseEntity<DiagnosisModels.DiagnosisView> get(@PathVariable long diagnosisId, AuthenticatedUser user) {
        return ResponseEntity.ok(service.get(user, diagnosisId));
    }
}
