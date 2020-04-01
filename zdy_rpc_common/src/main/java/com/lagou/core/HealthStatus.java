
package com.lagou.core;

public class HealthStatus {
    private String type;
    private boolean status;
    private String reason;

    public HealthStatus() {
    }

    public HealthStatus(String type, boolean status, String reason) {
        this.type = type;
        this.status = status;
        this.reason = reason;
    }

    public HealthStatus(String type, boolean status, String reason, String target, String addrs) {
        this.type = type;
        this.status = status;
        reason = reason + " [target=" + target + ",addrs=" + addrs + "]";
        this.reason = reason;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isStatus() {
        return this.status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getReason() {
        return this.reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
