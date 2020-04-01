package com.lagou.common;
public interface Alarm {
    String ALARM_TYPE_REGDIS = "001";
    String ALARM_TYPE_RPCSERVER = "002";
    String ALARM_TYPE_DISKIO = "003";
    String ALARM_TYPE_APM = "011";
    String ALARM_TYPE_QUEUEFULL = "012";
    String ALARM_TYPE_MONITOR = "013";
    String ALARM_TYPE_APMCFG = "900";
    String ALARM_TYPE_TRACABLE_POOL = "902";

    String getAlarmId(String type);

    void alarm(String type, String msg);

    void alarm(String type, String msg, String target, String addrs);

    String getAlarmPrefix();

    void alarm4rpc(String alarmId, String msg, String target, String addrs);
}