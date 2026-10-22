package com.bmhs.dsh;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.experimentcreation.LegacyWriteDisabled;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/dsh")
public class DshInstanceController {
    private final DshInstanceService service;

    public DshInstanceController(DshInstanceService service) {
        this.service = service;
    }

    @GetMapping("/status")
    public ResponseEntity<DshInstanceModels.InstanceView> status(AuthenticatedUser user) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.status(user));
    }

    @PostMapping("/launch")
    public ResponseEntity<DshInstanceModels.InstanceView> launch(AuthenticatedUser user) {
        throw LegacyWriteDisabled.exception();
    }

    @PostMapping("/stop")
    public ResponseEntity<Void> stop(AuthenticatedUser user) {
        throw LegacyWriteDisabled.exception();
    }

    /** Called only by the trusted reverse proxy's auth_request subrequest. */
    @GetMapping("/proxy-target")
    public ResponseEntity<Void> proxyTarget(AuthenticatedUser user, @RequestParam String routeKey) {
        int port = service.proxyPort(user, routeKey);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore())
                .header("X-Dsh-Upstream", "127.0.0.1:" + port).build();
    }
}
