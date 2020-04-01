//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.lagou.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyAlarm implements Alarm {
    static Logger log = LoggerFactory.getLogger(DummyAlarm.class);

    public DummyAlarm() {
    }

    public String getAlarmId(String type) {
        return "999" + type;
    }

    public void alarm(String type, String msg) {
        log.error("alarm message received, alarmId={},msg={}", this.getAlarmId(type), msg);
    }

    public void alarm(String type, String msg, String target, String addrs) {
        msg = msg + "[target=" + target + ",addrs=" + addrs + "]";
        this.alarm(type, msg);
    }

    public String getAlarmPrefix() {
        return "999";
    }

    public void alarm4rpc(String alarmId, String msg, String target, String addrs) {
        msg = msg + "[target=" + target + ",addrs=" + addrs + "]";
        log.error("alarm message received, alarmId={},msg={}", alarmId, msg);
    }
}
