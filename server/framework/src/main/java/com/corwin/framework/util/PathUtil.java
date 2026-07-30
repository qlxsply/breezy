package com.corwin.framework.util;

/**
 * Normalizes URL/path strings to a canonical form.
 * <p>
 * Ensures the result always starts with a single '{@code /}' and has no
 * trailing '{@code /}'.
 *
 * @author wl0180 2026/4/2
 */
public class PathUtil {

    private PathUtil() {
    }

    /**
     * Normalizes the given path:
     * <ul>
     *   <li>Returns {@code "/"} for null, blank, or empty input</li>
     *   <li>Prepends a leading '/' if missing</li>
     *   <li>Removes the trailing '/' if present (unless the result would be {@code "/"})</li>
     * </ul>
     *
     * @param path the raw path, may be null or blank
     * @return a normalized path starting with '/' and without trailing '/'
     */
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
