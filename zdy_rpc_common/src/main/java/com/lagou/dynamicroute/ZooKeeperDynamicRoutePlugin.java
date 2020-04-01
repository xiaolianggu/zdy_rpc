//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.lagou.dynamicroute;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.lagou.common.InitClose;
import com.lagou.common.Plugin;
import com.lagou.core.DynamicRouteConfig;
import com.lagou.core.DynamicRoutePlugin;


public class ZooKeeperDynamicRoutePlugin implements InitClose, DynamicRoutePlugin {
    static Logger log = LoggerFactory.getLogger(ZooKeeperDynamicRoutePlugin.class);
    String addrs;
    int interval = 15;
    CuratorFramework client;
    ConcurrentHashMap<String, String> versionCache = new ConcurrentHashMap();

    public ZooKeeperDynamicRoutePlugin() {
    }

    public void init() {
        this.client = CuratorFrameworkFactory.builder().connectString(this.addrs).sessionTimeoutMs(60000).connectionTimeoutMs(3000).canBeReadOnly(false).retryPolicy(new RetryOneTime(1000)).build();
        this.client.start();
    }

    public void config(String paramsStr) {
        Map<String, String> params = Plugin.defaultSplitParams(paramsStr);
        this.addrs = (String)params.get("addrs");
        String s = (String)params.get("intervalSeconds");
        if (!this.isEmpty(s)) {
            this.interval = Integer.parseInt(s);
        }

    }

    public void close() {
        this.client.close();
    }

    boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public int getRefreshIntervalSeconds() {
        return this.interval;
    }

    public DynamicRouteConfig getConfig(int serviceId, String serviceName, String group) {
        String path = "/dynamicroutes/" + group + "/" + serviceId + "/routes.json";
        String versionPath = path + ".version";
        String key = serviceId + "." + group;
        String oldVersion = (String)this.versionCache.get(key);
        String newVersion = null;

        try {
            byte[] bytes = (byte[])this.client.getData().forPath(versionPath);
            if (bytes == null) {
                return null;
            }

            newVersion = new String(bytes);
        } catch (Exception var13) {
            log.error("cannot get routes json version for service " + serviceName + ", exception=" + var13.getMessage());
            return null;
        }

        if (oldVersion != null && newVersion != null && oldVersion.equals(newVersion)) {
            return null;
        } else {
            DynamicRouteConfig config = null;

            try {
                byte[] bytes = (byte[])this.client.getData().forPath(path);
                if (bytes == null) {
                    return null;
                }

                String json = new String(bytes);
                config = (DynamicRouteConfig)JSON.parseObject(json, DynamicRouteConfig.class);
                if (config == null) {
                    log.error("invalid routes json for service " + serviceName + ", json=" + json);
                    return null;
                }
            } catch (Exception var12) {
                log.error("cannot get routes json for service " + serviceName + ", exception=" + var12.getMessage());
                return null;
            }

            this.versionCache.put(key, newVersion);
            return config;
        }
    }
}
