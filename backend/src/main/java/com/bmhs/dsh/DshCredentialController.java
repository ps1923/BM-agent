package com.bmhs.dsh;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plugin/auth")
public class DshCredentialController {
    private final DshTokenService tokenService;

    public DshCredentialController(DshTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping("/refresh")
    public ResponseEntity<CredentialResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        DshTokenService.CredentialPair pair = tokenService.refresh(request.refreshCredential());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new CredentialResponse(pair.access().token(), pair.access().expiresAt(), pair.refreshCredential()));
    }

    public record RefreshRequest(@NotBlank String refreshCredential) {}
    public record CredentialResponse(String credential, java.time.Instant expiresAt, String refreshCredential) {}
}
