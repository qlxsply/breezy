package com.corwin.framework.mybatis;

import com.corwin.framework.util.StrUtil;

/**
 * Utility for constructing SQL LIKE pattern arguments.
 *
 * @author wl0180
 * @since 2026/07/28
 */
public final class LikePatternUtils {

    private static final String WILDCARD = "%";

    private LikePatternUtils() {
        // Utility class
    }

    /**
     * Builds a contains-like pattern, e.g. {@code "abc" -> "%abc%"}.
     *
     * @param value the raw search term
     * @return the LIKE pattern, or {@code null} if the value is blank
     */
    public static String toContainsPattern(String value) {
        String normalized = StrUtil.trimToNull(value);
        return normalized == null ? null : WILDCARD + normalized + WILDCARD;
    }

    /**
     * Builds a prefix-like pattern, e.g. {@code "abc" -> "abc%"}.
     *
     * @param value the raw search term
     * @return the LIKE pattern, or {@code null} if the value is blank
     */
    public static String toPrefixPattern(String value) {
        String normalized = StrUtil.trimToNull(value);
        return normalized == null ? null : normalized + WILDCARD;
    }

}
