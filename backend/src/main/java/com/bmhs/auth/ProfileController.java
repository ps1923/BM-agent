package com.bmhs.auth;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.auth.AuthModels.ProfileUpdateRequest;
import com.bmhs.auth.AuthModels.ProfileView;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/profile")
public class ProfileController {
    private final ProfileService service;

    public ProfileController(ProfileService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<ProfileView> get(AuthenticatedUser user) {
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(service.get(user));
    }

    @PutMapping
    public ResponseEntity<ProfileView> update(AuthenticatedUser user,
                                              @Valid @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(service.update(user, request));
    }
}
