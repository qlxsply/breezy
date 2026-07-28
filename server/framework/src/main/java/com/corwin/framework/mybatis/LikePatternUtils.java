package com.corwin.framework.mybatis;

import com.corwin.framework.util.StrUtil;

/**
 * SQL LIKE 查询参数构造工具。
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
     * 构造包含匹配参数，例如："abc" -> "%abc%"。
     *
     * @param value 原始查询条件
     * @return LIKE 查询参数；当参数为空或仅包含空白字符时返回 null
     */
    public static String toContainsPattern(String value) {
        String normalized = StrUtil.trimToNull(value);
        return normalized == null ? null : WILDCARD + normalized + WILDCARD;
    }

    /**
     * 构造前缀匹配参数，例如："abc" -> "abc%"。
     *
     * @param value 原始查询条件
     * @return LIKE 查询参数；当参数为空或仅包含空白字符时返回 null
     */
    public static String toPrefixPattern(String value) {
        String normalized = StrUtil.trimToNull(value);
        return normalized == null ? null : normalized + WILDCARD;
    }

}
