package com.bmhs.dsh;

import java.time.Duration;

public record DshContainerSpec(String image, String dockerBinary, String apiBaseUrl,
                               String publicUrl, String trustedHost, String containerName,
                               String networkName, String volumeName, String workspaceRef,
                               String accessCredential, String refreshCredential,
                               double cpuLimit, int memoryLimitMb, int pidsLimit,
                               Duration startupTimeout, boolean insecureHttpEnabled) {}
