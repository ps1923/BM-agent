package com.bmhs.dsh;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "bm-hs.dsh.enabled", havingValue = "true")
public class DshSchedulingConfiguration {}
