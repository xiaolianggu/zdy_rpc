package com.lagou.core;

import java.util.List;
import java.util.Objects;

public class DynamicRouteConfig {
    int serviceId;
    boolean disabled;
    List<DynamicRouteConfig.AddrWeight> weights;
    List<DynamicRouteConfig.RouteRule> rules;

    public DynamicRouteConfig() {
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof DynamicRouteConfig)) {
            return false;
        } else {
            DynamicRouteConfig other = (DynamicRouteConfig)obj;
            boolean result = true;
            result = result && this.serviceId == other.serviceId;
            result = result && this.disabled == other.disabled;
            result = result && Objects.equals(this.weights, other.weights);
            result = result && Objects.equals(this.rules, other.rules);
            return result;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.serviceId, this.disabled, this.weights, this.rules});
    }

    public int getServiceId() {
        return this.serviceId;
    }

    public void setServiceId(int serviceId) {
        this.serviceId = serviceId;
    }

    public boolean isDisabled() {
        return this.disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public List<DynamicRouteConfig.AddrWeight> getWeights() {
        return this.weights;
    }

    public void setWeights(List<DynamicRouteConfig.AddrWeight> weights) {
        this.weights = weights;
    }

    public List<DynamicRouteConfig.RouteRule> getRules() {
        return this.rules;
    }

    public void setRules(List<DynamicRouteConfig.RouteRule> rules) {
        this.rules = rules;
    }

    public static class RouteRule implements Comparable<DynamicRouteConfig.RouteRule> {
        String from = "";
        String to = "";
        int priority;

        public RouteRule() {
        }

        public RouteRule(String from, String to, int priority) {
            this.from = from;
            this.to = to;
            this.priority = priority;
        }

        public int compareTo(DynamicRouteConfig.RouteRule rr) {
            if (this.priority < rr.priority) {
                return 1;
            } else {
                return this.priority > rr.priority ? -1 : 0;
            }
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (!(obj instanceof DynamicRouteConfig.RouteRule)) {
                return false;
            } else {
                DynamicRouteConfig.RouteRule other = (DynamicRouteConfig.RouteRule)obj;
                boolean result = true;
                result = result && Objects.equals(this.from, other.from);
                result = result && Objects.equals(this.to, other.to);
                result = result && this.priority == other.priority;
                return result;
            }
        }

        public int hashCode() {
            return Objects.hash(new Object[]{this.from, this.to, this.priority});
        }

        public String getFrom() {
            return this.from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getTo() {
            return this.to;
        }

        public void setTo(String to) {
            this.to = to;
        }

        public int getPriority() {
            return this.priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }
    }

    public static class AddrWeight {
        String addr = "";
        int weight;

        public AddrWeight() {
        }

        public AddrWeight(String addr, int weight) {
            this.addr = addr;
            this.weight = weight;
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (!(obj instanceof DynamicRouteConfig.AddrWeight)) {
                return false;
            } else {
                DynamicRouteConfig.AddrWeight other = (DynamicRouteConfig.AddrWeight)obj;
                boolean result = true;
                result = result && Objects.equals(this.addr, other.addr);
                result = result && this.weight == other.weight;
                return result;
            }
        }

        public int hashCode() {
            return Objects.hash(new Object[]{this.addr, this.weight});
        }

        public String getAddr() {
            return this.addr;
        }

        public void setAddr(String addr) {
            this.addr = addr;
        }

        public int getWeight() {
            return this.weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
        }
    }
}