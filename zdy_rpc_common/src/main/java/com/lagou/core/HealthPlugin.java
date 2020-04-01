package com.lagou.core;

import java.util.List;

public interface HealthPlugin {
    void healthCheck(List<HealthStatus> var1);
}