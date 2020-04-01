package com.lagou.common;

import java.util.HashMap;
import java.util.Map;

public interface Plugin {
    default void config(String params) {
    }

    static Map<String, String> splitParams(String params, String sep1, String sep2) {
        Map<String, String> m = new HashMap();
        if (params != null && !params.isEmpty()) {
            String[] ss = params.split(sep1);
            String[] var5 = ss;
            int var6 = ss.length;

            for(int var7 = 0; var7 < var6; ++var7) {
                String s = var5[var7];
                int p = s.indexOf(sep2);
                if (p > 0) {
                    String key = s.substring(0, p).trim();
                    String value = s.substring(p + 1).trim();
                    if (!key.isEmpty() && !value.isEmpty()) {
                        m.put(key, value);
                    }
                }
            }

            return m;
        } else {
            return m;
        }
    }

    static Map<String, String> defaultSplitParams(String params) {
        return splitParams(params, ";", "=");
    }
}