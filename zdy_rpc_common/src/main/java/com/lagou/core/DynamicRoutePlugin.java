package com.lagou.core;

import com.lagou.common.Plugin;

public interface DynamicRoutePlugin extends Plugin {
    int getRefreshIntervalSeconds();

    DynamicRouteConfig getConfig(int serviceId, String serviceName, String group);
}