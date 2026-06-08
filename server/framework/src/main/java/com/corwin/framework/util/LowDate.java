package com.corwin.framework.util;

import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.SysException;

import java.time.*;
import java.util.Date;
import java.util.TimeZone;

/**
 * 基于 {@link Date} 的兼容时间工具。
 * <p>
 * 该类主要服务于仍要求 {@link Date} 的旧接口、旧实体和三方库调用；新业务优先使用
 * {@link HighDate}，本类更多承担桥接与兼容职责。
 * <p>
 * 核心约定：
 * <ul>
 *     <li>{@link Date} 只表示时间线上的瞬时，不携带时区</li>
 *     <li>展示与解析“年月日时分秒”时，统一按系统默认时区处理</li>
 *     <li>按天查询优先使用 {@link #toStartOfDay(Date)} 与 {@link #toStartOfNextDay(Date)}</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>{@code
 * Date start = LowDate.toStartOfDay(new Date());
 * Date endExclusive = LowDate.toStartOfNextDay(new Date());
 * // 查询推荐：createdAt >= start && createdAt < endExclusive
 *
 * Date legacy = LowDate.toDate(HighDate.mockInstant());
 * Instant instant = LowDate.toInstant(legacy);
 * String text = LowDate.formatDateTime(legacy);
 * }</pre>
 *
 * @author Corwin 2025/10/19
 */
public final class LowDate {

    /* ----------------------- real (真实当前时间) ----------------------- */

    /**
     * 当前 Date（时间线），不依赖时区
     */
    public static Date realDateTime() {
        return Date.from(HighDate.realInstant());
    }

    /**
     * 当前“本地日”开始时间（00:00:00，系统默认时区）
     */
    /**
     * 当前真实瞬时对应的本地日期开始时间。
     */
    public static Date realDate() {
        return toStartOfDay(realDateTime());
    }

    /**
     * 当前“本地时间”部分（用 epochDay=0 的那一天承载时分秒；系统默认时区）
     * 说明：Date 不适合表达“纯时间”，这里只是兼容旧接口。
     */
    /**
     * 当前真实瞬时对应的本地时间部分。
     */
    public static Date realTime() {
        return toTimeOfDay(realDateTime());
    }

    /* ----------------------- mock (偏移后的当前时间) ----------------------- */

    /**
     * mock DateTime（= now + offsetSeconds），不依赖时区
     */
    public static Date mockDateTime() {
        return Date.from(HighDate.mockInstant());
    }

    /**
     * mock “本地日”开始时间（00:00:00，系统默认时区）
     */
    /**
     * 当前 mock 瞬时对应的本地日期开始时间。
     */
    public static Date mockDate() {
        return toStartOfDay(mockDateTime());
    }

    /**
     * mock “本地时间”部分（用 epochDay=0 承载，系统默认时区）
     */
    /**
     * 当前 mock 瞬时对应的本地时间部分。
     */
    public static Date mockTime() {
        return toTimeOfDay(mockDateTime());
    }

    /* ----------------------- of (timestamp -> date parts) ----------------------- */

    /**
     * 毫秒时间戳 -> Date
     */
    public static Date ofDateTime(long epochMillis) {
        return new Date(epochMillis);
    }

    /**
     * 毫秒时间戳 -> “本地日”开始时间（00:00:00，系统默认时区）
     */
    /**
     * 毫秒时间戳转为默认时区下的日期开始时间。
     */
    public static Date ofDate(long epochMillis) {
        return toStartOfDay(new Date(epochMillis));
    }

    /**
     * 毫秒时间戳 -> “本地时间”部分（用 epochDay=0 承载，系统默认时区）
     */
    /**
     * 毫秒时间戳转为默认时区下的时间部分。
     */
    public static Date ofTime(long epochMillis) {
        return toTimeOfDay(new Date(epochMillis));
    }

    /* ----------------------- parse (string -> Date) ----------------------- */

    /**
     * 解析 yyyy-MM-dd HH:mm:ss 为 Date（按系统默认时区解释）
     */
    public static Date parse(String str) {
        return toDate(HighDate.parse(str));
    }

    /**
     * 解析 yyyy-MM-dd 为 Date（按系统默认时区的当天 00:00:00）
     */
    public static Date parseDate(String str) {
        return toDate(HighDate.parseDate(str));
    }

    /**
     * 解析 HH:mm:ss 为 Date（按系统默认时区；用 epochDay=0 的那天承载时分秒）
     * 说明：这是“纯时间”的兼容表达；建议新代码用 LocalTime。
     */
    public static Date parseTime(String str) {
        return toDate(HighDate.parseTime(str));
    }

    /* ----------------------- format (Date -> string) ----------------------- */

    /**
     * 格式化 Date 为 yyyy-MM-dd HH:mm:ss（按系统默认时区展示）
     */
    public static String formatDateTime(Date date) {
        return HighDate.format(HighDate.ofDateTime(date.toInstant()));
    }

    /**
     * 格式化 Date 为 yyyy-MM-dd（按系统默认时区展示）
     */
    public static String formatDate(Date date) {
        return HighDate.format(HighDate.ofDate(date.toInstant()));
    }

    /**
     * 格式化 Date 为 HH:mm:ss（按系统默认时区展示）
     */
    public static String formatTime(Date date) {
        return HighDate.format(HighDate.ofTime(date.toInstant()));
    }

    /* ----------------------- epoch ----------------------- */

    /**
     * Date -> epoch seconds
     */
    public static long toEpochSeconds(Date date) {
        return date.toInstant().getEpochSecond();
    }

    /**
     * Date -> epoch millis
     */
    public static long toEpochMillis(Date date) {
        return date.getTime();
    }

    /* ----------------------- day boundary (system default zone) ----------------------- */

    /**
     * 转为当天“本地日”开始时间（00:00:00，系统默认时区）
     */
    public static Date toStartOfDay(Date dateTime) {
        return toDate(HighDate.toStartOfDay(dateTime.toInstant()));
    }

    /**
     * 转为次日“本地日”开始时间（00:00:00，系统默认时区）。
     * 查询范围推荐使用 [startOfDay, startOfNextDay) 半开区间，避免 <= endOfDay 的精度歧义。
     */
    public static Date toStartOfNextDay(Date dateTime) {
        return toDate(HighDate.toStartOfNextDay(dateTime.toInstant()));
    }

    /**
     * 转为当月第一天 00:00:00。
     */
    public static Date toStartOfMonth(Date dateTime) {
        return toDate(HighDate.toStartOfMonth(dateTime.toInstant()));
    }

    /**
     * 转为下月第一天 00:00:00。
     */
    public static Date toStartOfNextMonth(Date dateTime) {
        return toDate(HighDate.toStartOfNextMonth(dateTime.toInstant()));
    }

    /**
     * 转为当年第一天 00:00:00。
     */
    public static Date toStartOfYear(Date dateTime) {
        return toDate(HighDate.toStartOfYear(dateTime.toInstant()));
    }

    /**
     * 转为下一年第一天 00:00:00。
     */
    public static Date toStartOfNextYear(Date dateTime) {
        return toDate(HighDate.toStartOfNextYear(dateTime.toInstant()));
    }

    /**
     * 提取“本地时间”部分（时分秒毫秒），并用 epochDay=0 的那天承载返回 Date。
     * 说明：Date 不适合表达“纯时间”，这里只是为了兼容旧接口。
     */
    public static Date toTimeOfDay(Date dateTime) {
        LocalTime lt = HighDate.ofTime(dateTime.toInstant());
        LocalDate base = LocalDate.ofEpochDay(0);
        return Date.from(LocalDateTime.of(base, lt).atZone(zone()).toInstant());
    }

    /* ----------------------- timezone parsing (unchanged) ----------------------- */

    /**
     * 解析时区 ID。
     * <p>
     * 与 {@link TimeZone#getTimeZone(String)} 不同，非法值不会静默回退到 GMT，而是直接抛错。
     * <p>
     * 示例：{@code LowDate.parseTimeZone("Asia/Shanghai")}。
     */
    public static TimeZone parseTimeZone(String timeZoneId) {
        if (timeZoneId == null || timeZoneId.isBlank()) {
            throw new SysException("TimeZone ID 为空", BaseError.SERVICE_ERROR);
        }

        String id = timeZoneId.trim();
        TimeZone tz = TimeZone.getTimeZone(id);

        // TimeZone.getTimeZone 的坑：非法值会返回 GMT
        if (!tz.getID().equals(id) && !"GMT".equalsIgnoreCase(id)) {
            throw new SysException("TimeZone ID 无效: " + timeZoneId, BaseError.SERVICE_ERROR);
        }

        return tz;
    }

    /**
     * 将 {@link Instant} 转为 {@link Date}。
     */
    public static Date toDate(Instant instant) {
        if (instant == null) {
            return null;
        }
        return Date.from(instant);
    }

    /**
     * 将 {@link LocalDateTime} 转为 {@link Date}。
     */
    public static Date toDate(LocalDateTime dateTime) {
        return toDate(HighDate.toInstant(dateTime));
    }

    /**
     * 将 {@link LocalDate} 转为当天开始时刻对应的 {@link Date}。
     */
    public static Date toDate(LocalDate date) {
        return toDate(HighDate.toInstant(date));
    }

    /**
     * 将 {@link LocalTime} 转为仅承载“时间部分”的兼容 {@link Date}。
     */
    public static Date toDate(LocalTime time) {
        if (time == null) {
            return null;
        }
        return toDate(LocalDateTime.of(LocalDate.ofEpochDay(0), time));
    }

    /**
     * 将 {@link Date} 转回 {@link Instant}。
     */
    public static Instant toInstant(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant();
    }

    /**
     * 将 {@link Date} 转为默认时区下的本地日期时间。
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return HighDate.ofDateTime(date.toInstant());
    }

    /**
     * 将 {@link Date} 转为默认时区下的本地日期。
     */
    public static LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return HighDate.ofDate(date.toInstant());
    }

    /**
     * 将 {@link Date} 转为默认时区下的本地时间。
     */
    public static LocalTime toLocalTime(Date date) {
        if (date == null) {
            return null;
        }
        return HighDate.ofTime(date.toInstant());
    }

    /**
     * 按自定义 pattern 格式化日期时间。
     */
    public static String formatDateTime(Date date, String pattern) {
        return HighDate.format(toLocalDateTime(date), pattern);
    }

    /**
     * 按自定义 pattern 格式化日期。
     */
    public static String formatDate(Date date, String pattern) {
        return HighDate.format(toLocalDate(date), pattern);
    }

    /**
     * 按自定义 pattern 格式化时间。
     */
    public static String formatTime(Date date, String pattern) {
        return HighDate.format(toLocalTime(date), pattern);
    }

    /**
     * 按自定义 pattern 解析日期时间字符串。
     */
    public static Date parse(String str, String pattern) {
        return toDate(HighDate.parse(str, pattern));
    }

    /**
     * 按自定义 pattern 解析日期字符串。
     */
    public static Date parseDate(String str, String pattern) {
        return toDate(HighDate.parseDate(str, pattern));
    }

    /**
     * 按自定义 pattern 解析时间字符串。
     */
    public static Date parseTime(String str, String pattern) {
        return toDate(HighDate.parseTime(str, pattern));
    }

    private static ZoneId zone() {
        return HighDate.zone();
    }

    private LowDate() {
    }

}
