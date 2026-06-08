package com.corwin.framework.util;

import com.corwin.framework.config.DefaultConfigKeys;
import com.corwin.framework.config.ConfigRegistry;
import com.corwin.framework.constant.DateTimePatterns;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * 基于 {@code java.time} 的主时间工具。
 * <p>
 * 核心约定：
 * <ul>
 *     <li>时间线统一以 {@link Instant} 为准，适合存储、比较、传输</li>
 *     <li>展示与输入统一使用 {@link LocalDateTime} / {@link LocalDate} / {@link LocalTime}</li>
 *     <li>未显式指定时区时，统一使用 {@link ZoneId#systemDefault()}</li>
 *     <li>时间范围查询推荐使用半开区间：{@code [startInclusive, endExclusive)}</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>{@code
 * Instant now = HighDate.mockInstant();
 * Instant dayStart = HighDate.toStartOfDay(now);
 * Instant nextDayStart = HighDate.toStartOfNextDay(now);
 * // 查询推荐：createdAt >= dayStart && createdAt < nextDayStart
 *
 * LocalDateTime local = HighDate.ofDateTime(now);
 * String text = HighDate.format(local);
 * LocalDateTime parsed = HighDate.parse("2026-03-14 08:30:00");
 * }</pre>
 *
 * @author Corwin 2026/2/1
 */
public final class HighDate {

    /**
     * 返回默认时区。
     * <p>
     * 当业务未显式提供用户时区时，统一走该入口，避免在项目中直接散落调用
     * {@code ZoneId.systemDefault()}。
     */
    public static ZoneId zone() {
        return ZoneId.systemDefault();
    }

    /* ----------------------- real (真实当前时间) ----------------------- */

    /**
     * 当前瞬时（时间线），不依赖时区
     */
    public static Instant realInstant() {
        return Instant.now();
    }

    /**
     * 当前本地日期时间（系统默认时区）
     */
    /**
     * 当前真实瞬时在默认时区下对应的本地日期时间。
     */
    public static LocalDateTime realDateTime() {
        return ofDateTime(realInstant());
    }

    /**
     * 当前本地日期（系统默认时区）
     */
    /**
     * 当前真实瞬时在默认时区下对应的本地日期。
     */
    public static LocalDate realDate() {
        return realDateTime().toLocalDate();
    }

    /**
     * 当前本地时间（系统默认时区）
     */
    /**
     * 当前真实瞬时在默认时区下对应的本地时间。
     */
    public static LocalTime realTime() {
        return realDateTime().toLocalTime();
    }

    /**
     * 当前时间戳（毫秒）
     */
    public static long realTimestampMillis() {
        return realInstant().toEpochMilli();
    }

    /* ----------------------- mock (偏移后的当前时间) ----------------------- */

    /**
     * mock 瞬时（= now + offsetSeconds），不依赖时区
     */
    public static Instant mockInstant() {
        long offsetSeconds = ConfigRegistry.longV(DefaultConfigKeys.TIME_OFFSET); // 约定：秒
        return Instant.now().plusSeconds(offsetSeconds);
    }

    /**
     * mock 本地日期时间（系统默认时区）
     */
    /**
     * 当前 mock 瞬时在默认时区下对应的本地日期时间。
     */
    public static LocalDateTime mockDateTime() {
        return ofDateTime(mockInstant());
    }

    /**
     * mock 本地日期（系统默认时区）
     */
    /**
     * 当前 mock 瞬时在默认时区下对应的本地日期。
     */
    public static LocalDate mockDate() {
        return mockDateTime().toLocalDate();
    }

    /**
     * mock 本地时间（系统默认时区）
     */
    /**
     * 当前 mock 瞬时在默认时区下对应的本地时间。
     */
    public static LocalTime mockTime() {
        return mockDateTime().toLocalTime();
    }

    /**
     * mock 时间戳（毫秒）
     */
    public static long mockTimestampMillis() {
        return mockInstant().toEpochMilli();
    }

    /* ----------------------- convert (timestamp <-> local) ----------------------- */

    /**
     * 毫秒时间戳 -> 本地日期时间（系统默认时区）
     */
    /**
     * 将毫秒时间戳转为 {@link Instant}。
     */
    public static Instant ofInstant(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis);
    }

    /**
     * 将秒时间戳转为 {@link Instant}。
     */
    public static Instant ofInstantSeconds(long epochSeconds) {
        return Instant.ofEpochSecond(epochSeconds);
    }

    /**
     * 将 {@link Instant} 转为默认时区下的本地日期时间。
     */
    public static LocalDateTime ofDateTime(long epochMillis) {
        return ofDateTime(ofInstant(epochMillis));
    }

    /**
     * 将 {@link Instant} 转为默认时区下的本地日期时间。
     */
    public static LocalDateTime ofDateTime(Instant instant) {
        return ofDateTime(instant, zone());
    }

    /**
     * 将 {@link Instant} 转为指定时区下的本地日期时间。
     */
    public static LocalDateTime ofDateTime(Instant instant, ZoneId zoneId) {
        return LocalDateTime.ofInstant(instant, zoneId);
    }

    /**
     * 毫秒时间戳 -> 本地日期（系统默认时区）
     */
    public static LocalDate ofDate(long epochMillis) {
        return ofDateTime(epochMillis).toLocalDate();
    }

    /**
     * 将 {@link Instant} 转为默认时区下的本地日期。
     */
    public static LocalDate ofDate(Instant instant) {
        return ofDateTime(instant).toLocalDate();
    }

    /**
     * 将 {@link Instant} 转为指定时区下的本地日期。
     */
    public static LocalDate ofDate(Instant instant, ZoneId zoneId) {
        return ofDateTime(instant, zoneId).toLocalDate();
    }

    /**
     * 毫秒时间戳 -> 本地时间（系统默认时区）
     */
    public static LocalTime ofTime(long epochMillis) {
        return ofDateTime(epochMillis).toLocalTime();
    }

    /**
     * 将 {@link Instant} 转为默认时区下的本地时间。
     */
    public static LocalTime ofTime(Instant instant) {
        return ofDateTime(instant).toLocalTime();
    }

    /**
     * 将 {@link Instant} 转为指定时区下的本地时间。
     */
    public static LocalTime ofTime(Instant instant, ZoneId zoneId) {
        return ofDateTime(instant, zoneId).toLocalTime();
    }

    /**
     * 将 {@link Instant} 转为默认时区下的 {@link ZonedDateTime}。
     */
    public static ZonedDateTime ofZonedDateTime(Instant instant) {
        return instant.atZone(zone());
    }

    /**
     * 将 {@link Instant} 转为指定时区下的 {@link ZonedDateTime}。
     */
    public static ZonedDateTime ofZonedDateTime(Instant instant, ZoneId zoneId) {
        return instant.atZone(zoneId);
    }

    /**
     * 将默认时区下的本地日期时间转为 {@link Instant}。
     */
    public static Instant toInstant(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(zone()).toInstant();
    }

    /**
     * 将指定时区下的本地日期时间转为 {@link Instant}。
     */
    public static Instant toInstant(LocalDateTime dateTime, ZoneId zoneId) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(zoneId).toInstant();
    }

    /**
     * 将默认时区下的日期转为当天 00:00:00 对应的 {@link Instant}。
     */
    public static Instant toInstant(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atStartOfDay(zone()).toInstant();
    }

    /**
     * 将指定时区下的日期转为当天 00:00:00 对应的 {@link Instant}。
     */
    public static Instant toInstant(LocalDate date, ZoneId zoneId) {
        if (date == null) {
            return null;
        }
        return date.atStartOfDay(zoneId).toInstant();
    }

    /**
     * 将默认时区下的日期和时间拼装为 {@link Instant}。
     */
    public static Instant toInstant(LocalDate date, LocalTime time) {
        return toInstant(date, time, zone());
    }

    /**
     * 将指定时区下的日期和时间拼装为 {@link Instant}。
     * <p>
     * 适用于表单拆分输入“日期 + 时间”的场景。
     */
    public static Instant toInstant(LocalDate date, LocalTime time, ZoneId zoneId) {
        if (date == null || time == null) {
            return null;
        }
        return LocalDateTime.of(date, time).atZone(zoneId).toInstant();
    }

    /**
     * 本地日期时间（系统默认时区）-> 毫秒时间戳
     */
    public static long toEpochMillis(LocalDateTime ldt) {
        if (ldt == null) {
            return 0L;
        }
        return toInstant(ldt).toEpochMilli();
    }

    /**
     * 本地日期时间（系统默认时区）-> 秒时间戳
     */
    public static long toEpochSeconds(LocalDateTime ldt) {
        if (ldt == null) {
            return 0L;
        }
        return toInstant(ldt).getEpochSecond();
    }

    /**
     * 将 {@link Instant} 转为毫秒时间戳。
     */
    public static long toEpochMillis(Instant instant) {
        if (instant == null) {
            return 0L;
        }
        return instant.toEpochMilli();
    }

    /**
     * 将 {@link Instant} 转为秒时间戳。
     */
    public static long toEpochSeconds(Instant instant) {
        if (instant == null) {
            return 0L;
        }
        return instant.getEpochSecond();
    }

    /* ----------------------- range / boundary ----------------------- */

    /**
     * 归一化到当天开始时间 `00:00:00`。
     */
    public static LocalDateTime toStartOfDay(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalDate().atStartOfDay();
    }

    /**
     * 将日期转为当天开始时间 `00:00:00`。
     */
    public static LocalDateTime toStartOfDay(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atStartOfDay();
    }

    /**
     * 归一化到次日开始时间 `00:00:00`。
     * <p>
     * 推荐作为按天查询的排他上界。
     */
    public static LocalDateTime toStartOfNextDay(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalDate().plusDays(1).atStartOfDay();
    }

    /**
     * 将日期转为次日开始时间 `00:00:00`。
     */
    public static LocalDateTime toStartOfNextDay(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.plusDays(1).atStartOfDay();
    }

    /**
     * 返回当天最后一个纳秒时刻。
     * <p>
     * 仅用于兼容“日结束”语义，查询上界仍优先推荐 {@link #toStartOfNextDay(LocalDateTime)}。
     */
    public static LocalDateTime toEndOfDay(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return toStartOfNextDay(dateTime).minusNanos(1);
    }

    /**
     * 返回指定日期当天最后一个纳秒时刻。
     */
    public static LocalDateTime toEndOfDay(LocalDate date) {
        if (date == null) {
            return null;
        }
        return toStartOfNextDay(date).minusNanos(1);
    }

    /**
     * 将 {@link Instant} 归一化到默认时区的当天开始。
     */
    public static Instant toStartOfDay(Instant instant) {
        return toStartOfDay(instant, zone());
    }

    /**
     * 将 {@link Instant} 归一化到指定时区的当天开始。
     */
    public static Instant toStartOfDay(Instant instant, ZoneId zoneId) {
        if (instant == null) {
            return null;
        }
        return ofDate(instant, zoneId).atStartOfDay(zoneId).toInstant();
    }

    /**
     * 将 {@link Instant} 归一化到默认时区的次日开始。
     */
    public static Instant toStartOfNextDay(Instant instant) {
        return toStartOfNextDay(instant, zone());
    }

    /**
     * 将 {@link Instant} 归一化到指定时区的次日开始。
     */
    public static Instant toStartOfNextDay(Instant instant, ZoneId zoneId) {
        if (instant == null) {
            return null;
        }
        return ofDate(instant, zoneId).plusDays(1).atStartOfDay(zoneId).toInstant();
    }

    /**
     * 返回默认时区下当天最后一个纳秒时刻。
     */
    public static Instant toEndOfDay(Instant instant) {
        return toEndOfDay(instant, zone());
    }

    /**
     * 返回指定时区下当天最后一个纳秒时刻。
     */
    public static Instant toEndOfDay(Instant instant, ZoneId zoneId) {
        Instant nextDayStart = toStartOfNextDay(instant, zoneId);
        if (nextDayStart == null) {
            return null;
        }
        return nextDayStart.minusNanos(1);
    }

    /**
     * 归一化到当月第一天 `00:00:00`。
     */
    public static LocalDateTime toStartOfMonth(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalDate().withDayOfMonth(1).atStartOfDay();
    }

    /**
     * 归一化到下月第一天 `00:00:00`。
     */
    public static LocalDateTime toStartOfNextMonth(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalDate().withDayOfMonth(1).plusMonths(1).atStartOfDay();
    }

    /**
     * 归一化到当年第一天 `00:00:00`。
     */
    public static LocalDateTime toStartOfYear(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalDate().withDayOfYear(1).atStartOfDay();
    }

    /**
     * 归一化到下一年第一天 `00:00:00`。
     */
    public static LocalDateTime toStartOfNextYear(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalDate().withDayOfYear(1).plusYears(1).atStartOfDay();
    }

    /**
     * 将 {@link Instant} 归一化到默认时区的当月开始。
     */
    public static Instant toStartOfMonth(Instant instant) {
        return toStartOfMonth(instant, zone());
    }

    /**
     * 将 {@link Instant} 归一化到指定时区的当月开始。
     */
    public static Instant toStartOfMonth(Instant instant, ZoneId zoneId) {
        if (instant == null) {
            return null;
        }
        LocalDate date = ofDate(instant, zoneId).withDayOfMonth(1);
        return date.atStartOfDay(zoneId).toInstant();
    }

    /**
     * 将 {@link Instant} 归一化到默认时区的下月开始。
     */
    public static Instant toStartOfNextMonth(Instant instant) {
        return toStartOfNextMonth(instant, zone());
    }

    /**
     * 将 {@link Instant} 归一化到指定时区的下月开始。
     */
    public static Instant toStartOfNextMonth(Instant instant, ZoneId zoneId) {
        if (instant == null) {
            return null;
        }
        LocalDate date = ofDate(instant, zoneId).withDayOfMonth(1).plusMonths(1);
        return date.atStartOfDay(zoneId).toInstant();
    }

    /**
     * 将 {@link Instant} 归一化到默认时区的当年开始。
     */
    public static Instant toStartOfYear(Instant instant) {
        return toStartOfYear(instant, zone());
    }

    /**
     * 将 {@link Instant} 归一化到指定时区的当年开始。
     */
    public static Instant toStartOfYear(Instant instant, ZoneId zoneId) {
        if (instant == null) {
            return null;
        }
        LocalDate date = ofDate(instant, zoneId).withDayOfYear(1);
        return date.atStartOfDay(zoneId).toInstant();
    }

    /**
     * 将 {@link Instant} 归一化到默认时区的下一年开始。
     */
    public static Instant toStartOfNextYear(Instant instant) {
        return toStartOfNextYear(instant, zone());
    }

    /**
     * 将 {@link Instant} 归一化到指定时区的下一年开始。
     */
    public static Instant toStartOfNextYear(Instant instant, ZoneId zoneId) {
        if (instant == null) {
            return null;
        }
        LocalDate date = ofDate(instant, zoneId).withDayOfYear(1).plusYears(1);
        return date.atStartOfDay(zoneId).toInstant();
    }

    /**
     * 计算半开区间 {@code [startInclusive, endExclusive)} 的毫秒差。
     */
    public static long betweenMillis(Instant startInclusive, Instant endExclusive) {
        Objects.requireNonNull(startInclusive, "startInclusive required");
        Objects.requireNonNull(endExclusive, "endExclusive required");
        return endExclusive.toEpochMilli() - startInclusive.toEpochMilli();
    }

    /**
     * 计算半开区间 {@code [startInclusive, endExclusive)} 的秒差。
     */
    public static long betweenSeconds(Instant startInclusive, Instant endExclusive) {
        Objects.requireNonNull(startInclusive, "startInclusive required");
        Objects.requireNonNull(endExclusive, "endExclusive required");
        return endExclusive.getEpochSecond() - startInclusive.getEpochSecond();
    }

    /**
     * 返回两个瞬时中更早的一个；允许任一参数为 {@code null}。
     */
    public static Instant min(Instant left, Instant right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isBefore(right) ? left : right;
    }

    /**
     * 返回两个瞬时中更晚的一个；允许任一参数为 {@code null}。
     */
    public static Instant max(Instant left, Instant right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isAfter(right) ? left : right;
    }

    /* ----------------------- parse / format ----------------------- */
    // 注意：LocalDateTime/LocalDate/LocalTime 本身不带时区信息；
    // parse/format 只是字符串 <-> 本地时间对象，与时区无关（除非字符串里包含 offset/zone）。

    /**
     * 按默认格式 {@code yyyy-MM-dd HH:mm:ss} 解析本地日期时间。
     */
    public static LocalDateTime parse(String str) {
        return parse(str, DateTimePatterns.DATE_TIME_FMT);
    }

    /**
     * 按默认格式 {@code yyyy-MM-dd} 解析本地日期。
     */
    public static LocalDate parseDate(String str) {
        return parseDate(str, DateTimePatterns.DATE_FMT);
    }

    /**
     * 按默认格式 {@code HH:mm:ss} 解析本地时间。
     */
    public static LocalTime parseTime(String str) {
        return parseTime(str, DateTimePatterns.TIME_FMT);
    }

    /**
     * 使用指定格式器解析本地日期时间。
     */
    public static LocalDateTime parse(String str, DateTimeFormatter formatter) {
        return LocalDateTime.parse(str, formatter);
    }

    /**
     * 使用指定 pattern 解析本地日期时间。
     * <p>
     * 示例：{@code HighDate.parse("2026/03/14 08:30", "yyyy/MM/dd HH:mm")}。
     */
    public static LocalDateTime parse(String str, String pattern) {
        return parse(str, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 使用指定格式器解析本地日期。
     */
    public static LocalDate parseDate(String str, DateTimeFormatter formatter) {
        return LocalDate.parse(str, formatter);
    }

    /**
     * 使用指定 pattern 解析本地日期。
     */
    public static LocalDate parseDate(String str, String pattern) {
        return parseDate(str, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 使用指定格式器解析本地时间。
     */
    public static LocalTime parseTime(String str, DateTimeFormatter formatter) {
        return LocalTime.parse(str, formatter);
    }

    /**
     * 使用指定 pattern 解析本地时间。
     */
    public static LocalTime parseTime(String str, String pattern) {
        return parseTime(str, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 按默认格式 {@code yyyy-MM-dd HH:mm:ss} 格式化本地日期时间。
     */
    public static String format(LocalDateTime ldt) {
        return format(ldt, DateTimePatterns.DATE_TIME_FMT);
    }

    /**
     * 按默认格式 {@code yyyy-MM-dd} 格式化本地日期。
     */
    public static String format(LocalDate ld) {
        return format(ld, DateTimePatterns.DATE_FMT);
    }

    /**
     * 按默认格式 {@code HH:mm:ss} 格式化本地时间。
     */
    public static String format(LocalTime lt) {
        return format(lt, DateTimePatterns.TIME_FMT);
    }

    /**
     * 使用指定格式器格式化本地日期时间。
     */
    public static String format(LocalDateTime ldt, DateTimeFormatter formatter) {
        return ldt.format(formatter);
    }

    /**
     * 使用指定 pattern 格式化本地日期时间。
     */
    public static String format(LocalDateTime ldt, String pattern) {
        return format(ldt, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 使用指定格式器格式化本地日期。
     */
    public static String format(LocalDate ld, DateTimeFormatter formatter) {
        return ld.format(formatter);
    }

    /**
     * 使用指定 pattern 格式化本地日期。
     */
    public static String format(LocalDate ld, String pattern) {
        return format(ld, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 使用指定格式器格式化本地时间。
     */
    public static String format(LocalTime lt, DateTimeFormatter formatter) {
        return lt.format(formatter);
    }

    /**
     * 使用指定 pattern 格式化本地时间。
     */
    public static String format(LocalTime lt, String pattern) {
        return format(lt, DateTimeFormatter.ofPattern(pattern));
    }

    private HighDate() {
    }

}
