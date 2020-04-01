package com.lagou.core;
public interface Registry {
    int getCheckIntervalSeconds();

    void register(int serviceId, String serviceName, String group, String addr);

    void deregister(int serviceId, String serviceName, String group, String addr);

    String discover(int serviceId, String serviceName, String group);
}
