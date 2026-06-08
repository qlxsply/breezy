package com.corwin.framework.util;

/**
 *
 * @author wl0180 2026/4/2
 */
public class PathUtil {

    private PathUtil() {
    }

    public static String normalize(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String value = path.trim();
        if (!value.startsWith("/")) {
            value = "/" + value;
        }
        if (value.length() > 1 && value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

}
