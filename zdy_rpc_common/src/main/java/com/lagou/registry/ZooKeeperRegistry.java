package com.lagou.registry;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLBackgroundPathAndBytesable;
import org.apache.curator.retry.RetryOneTime;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.lagou.common.Alarm;
import com.lagou.common.AlarmAware;
import com.lagou.common.DummyAlarm;
import com.lagou.common.InitClose;
import com.lagou.core.DumpPlugin;
import com.lagou.core.DynamicRouteConfig;
import com.lagou.core.DynamicRoutePlugin;
import com.lagou.core.HealthPlugin;
import com.lagou.core.HealthStatus;
import com.lagou.core.Registry;

public class ZooKeeperRegistry implements Registry, InitClose,DynamicRoutePlugin, DumpPlugin, HealthPlugin, AlarmAware{
    static Logger log = LoggerFactory.getLogger(ZooKeeperRegistry.class);
    String addrs;
    boolean enableRegist = true;
    boolean enableDiscover = true;
    int interval = 15;
    CuratorFramework client;
    int errorTimeToAlarm = 60;
    volatile long lastErrorTime = 0L;
    ConcurrentHashMap<String, String> versionCache = new ConcurrentHashMap<String, String>();
    Alarm alarm = new DummyAlarm();
    public ZooKeeperRegistry() {
    }

    public void init() {
        this.client = CuratorFrameworkFactory.builder().connectString(this.addrs).sessionTimeoutMs(60000).connectionTimeoutMs(3000).canBeReadOnly(false).retryPolicy(new RetryOneTime(1000)).build();
        this.client.start();
    }


    public void close() {
        this.client.close();
    }

    boolean needAlarm() {
        if (this.lastErrorTime == 0L) {
            return false;
        } else {
            long now = System.currentTimeMillis();
            return now - this.lastErrorTime >= (long)(this.errorTimeToAlarm * 1000);
        }
    }

    void updateLastErrorTime() {
        if (this.lastErrorTime == 0L) {
            this.lastErrorTime = System.currentTimeMillis();
        }

    }

    boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public int getCheckIntervalSeconds() {
        return this.interval;
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
        } catch (Exception e) {
            log.error("cannot get routes json version for service " + serviceName + ", exception=" + e.getMessage());
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
                config = JSON.parseObject(json, DynamicRouteConfig.class);
                if (config == null) {
                    log.error("invalid routes json for service " + serviceName + ", json=" + json);
                    return null;
                }
            } catch (Exception e) {
                log.error("cannot get routes json for service " + serviceName + ", exception=" + e.getMessage());
                return null;
            }

            this.versionCache.put(key, newVersion);
            return config;
        }
    }

    public void register(int serviceId, String serviceName, String group, String addr) {
        if (this.enableRegist) {
            String path = "/services/" + group + "/" + serviceId + "/" + addr;
            HashMap<String, Object> meta = new HashMap();
            meta.put("addr", addr);
            meta.put("group", group);
            meta.put("serviceName", serviceName);
            String data = JSON.toJSONString(meta);

            try {
                ((ACLBackgroundPathAndBytesable)this.client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL)).forPath(path, data.getBytes());
                this.lastErrorTime = 0L;
            } catch (Exception e) {
                if (e.getMessage().indexOf("NodeExists") < 0) {
                    this.updateLastErrorTime();
                    log.error("cannot register service " + serviceName + ", exception=" + e.getMessage());
                }
            }

        }
    }

    public void deregister(int serviceId, String serviceName, String group, String addr) {
        if (this.enableRegist) {
            String path = "/services/" + group + "/" + serviceId + "/" + addr;

            try {
                this.client.delete().forPath(path);
                this.lastErrorTime = 0L;
            } catch (Exception e) {
                this.updateLastErrorTime();
                log.error("cannot deregister service " + serviceName + ", exception=" + e.getMessage());
            }

        }
    }

    public String discover(int serviceId, String serviceName, String group) {
        if (!this.enableDiscover) {
            return null;
        } else {
            String path = "/services/" + group + "/" + serviceId;
            List list = null;

            try {
                list = (List)this.client.getChildren().forPath(path);
                this.lastErrorTime = 0L;
            } catch (Exception e) {
                log.error("cannot discover service " + serviceName + ", exception=" + e.getMessage());
                this.updateLastErrorTime();
                return null;
            }

            TreeSet<String> set = new TreeSet<String>();
            Iterator<String> var7 = list.iterator();

            String s;
            while(var7.hasNext()) {
                s = (String)var7.next();
                set.add(s);
            }

            StringBuilder b = new StringBuilder();

            String key;
            for(Iterator var12 = set.iterator(); var12.hasNext(); b.append(key)) {
                key = (String)var12.next();
                if (b.length() > 0) {
                    b.append(",");
                }
            }

            s = b.toString();
            return s;
        }
    }

    public void healthCheck(List<HealthStatus> list) {
        if (this.needAlarm()) {
            String alarmId = this.alarm.getAlarmId("001");
            list.add(new HealthStatus(alarmId, false, "zk_registry connect failed", "zookeeper", this.addrs.replaceAll(",", "#")));
        }
    }

    public void dump(Map<String, Object> metrics) {
        if (this.needAlarm()) {
            this.alarm.alarm("001", "zk_registry connect failed", "zookeeper", this.addrs.replaceAll(",", "#"));
            metrics.put("rpc.zookeeper.errorCount", 1);
        }
    }

    public void setAlarm(Alarm alarm) {
        this.alarm = alarm;
    }
}