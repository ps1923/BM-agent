package com.bmhs.dsh;

public interface DshContainerRuntime {
    DshInstanceModels.RuntimeHandle start(DshContainerSpec spec);
    String currentLaunchUrl(String dockerBinary, String publicUrl, String containerId, java.time.Duration timeout);
    void stop(String dockerBinary, String containerId, String networkName, java.time.Duration timeout);
}
