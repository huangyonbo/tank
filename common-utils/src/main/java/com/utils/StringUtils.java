package com.utils;

/**
 * @Author HYB
 * @Date 2026/4/9
 * @Time 15:40
 * @Desc
 */
public class StringUtils {

    public static long luaLong(Object o) {
        if (o instanceof Long) {
            return (Long) o;
        }
        if (o instanceof Integer) {
            return ((Integer) o).longValue();
        }
        if (o instanceof byte[]) {
            try {
                return Long.parseLong(new String((byte[]) o));
            } catch (Exception ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    public static int parseIntSafe(String s) {
        if (s == null) {
            return 0;
        }
        try {
            return Integer.parseInt(s);
        } catch (Exception ignored) {
            return 0;
        }
    }

    public static long parseLongSafe(String s) {
        if (s == null || s.trim().isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(s.trim());
        } catch (Exception ignored) {
            return 0L;
        }
    }

    public static boolean parseBooleanSafe(String s) {
        if (s == null) {
            return false;
        }
        return "true".equalsIgnoreCase(s.trim()) || "1".equals(s.trim());
    }

    public static String parseStringSafe(String s) {
        return s == null ? "" : s;
    }
}
