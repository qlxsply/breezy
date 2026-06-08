package com.corwin.framework.constant;

import java.time.format.DateTimeFormatter;

/**
 * 日期时间格式常量。
 *
 * @author Corwin 2026/3/23
 */
public final class DateTimePatterns {

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public static final String DATE_PATTERN = "yyyy-MM-dd";

    public static final String TIME_PATTERN = "HH:mm:ss";

    public static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern(DATE_PATTERN);

    public static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern(TIME_PATTERN);

    private DateTimePatterns() {
    }
}
