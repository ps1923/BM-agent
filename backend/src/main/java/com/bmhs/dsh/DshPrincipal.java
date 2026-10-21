package com.bmhs.dsh;

import java.time.Instant;

public record DshPrincipal(long studentId, long instanceId, Instant expiresAt) {}
