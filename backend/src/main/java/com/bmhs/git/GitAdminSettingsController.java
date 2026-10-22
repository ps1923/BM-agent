package com.bmhs.git;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.bmhs.git.GitAdminSettingsModels.SettingsRequest;
import static com.bmhs.git.GitAdminSettingsModels.SettingsView;

@RestController
@RequestMapping("/api/admin/integrations/gitea")
public class GitAdminSettingsController {
    private final GitAdminSettingsService service;

    public GitAdminSettingsController(GitAdminSettingsService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<SettingsView> get(AuthenticatedUser user) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.get(user));
    }

    @PutMapping
    public ResponseEntity<SettingsView> save(AuthenticatedUser user, @Valid @RequestBody SettingsRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.save(user, request));
    }
}
