package com.bmhs.dsh;

import java.time.Instant;

public final class DshInstanceModels {
    private DshInstanceModels() {}

    public record Instance(long id, long studentId, String routeKey, String containerId,
                           String storageVolume, Integer proxyPort, String status,
                           double cpuLimit, int memoryLimitMb, int pidsLimit, Instant lastActiveAt) {}

    public record InstanceView(String status, String launchUrl, Instant lastActiveAt) {}

    public record RuntimeHandle(String containerId, int proxyPort, String launchUrl) {}
}
