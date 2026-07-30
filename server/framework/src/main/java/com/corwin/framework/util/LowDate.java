package com.corwin.framework.util;

import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.SysException;

import java.time.*;
import java.util.Date;
import java.util.TimeZone;

/**
 * {@link Date}-based compatibility date/time utility.
 * <p>
 * This class mainly serves legacy interfaces, entities, and third-party
 * libraries that still require {@link Date}. New code should prefer
 * {@link HighDate}; this class acts as a bridge.
 * <p>
 * Core conventions:
 * <ul>
 *   <li>{@link Date} represents an instant on the timeline; it carries no
 *       time zone information</li>
 *   <li>Display and parsing of "year-month-day hour:minute:second" are
 *       always handled in the system default time zone</li>
 *   <li>Day-range queries should use {@link #toStartOfDay(Date)} and
 *       {@link #toStartOfNextDay(Date)} with a half-open interval</li>
 * </ul>
 *
 * <pre>{@code
 * Date start = LowDate.toStartOfDay(new Date());
 * Date endExclusive = LowDate.toStartOfNextDay(new Date());
 * // Query: createdAt >= start && createdAt < endExclusive
 *
 * Date legacy = LowDate.toDate(HighDate.mockInstant());
 * Instant instant = LowDate.toInstant(legacy);
 * String text = LowDate.formatDateTime(legacy);
 * }</pre>
 *
 * @author Corwin 2025/10/19
 */
public final class LowDate {

    /* ----------------------- real (actual current time) ----------------------- */

    /**
     * Returns the current {@link Date} instance (timeline instant).
     * Does not depend on time zone.
     */
    public static Date realDateTime() {
        return Date.from(HighDate.realInstant());
    }

    /**
     * Returns the start-of-day of the current local date in the system
     * default time zone as a {@link Date}.
     */
    public static Date realDate() {
        return toStartOfDay(realDateTime());
    }

    /**
     * Returns the local time-of-day portion of the current instant,
     * carried on the epoch-day-0 date.
     * <p>
     * Note: {@link Date} is not suitable for representing "pure time";
     * this method exists only for legacy API compatibility.
     */
    public static Date realTime() {
        return toTimeOfDay(realDateTime());
    }

    /* ----------------------- mock (offset current time) ----------------------- */

    /**
     * Returns the mocked current {@link Date} (= now + offsetSeconds).
     * Does not depend on time zone.
     */
    public static Date mockDateTime() {
        return Date.from(HighDate.mockInstant());
    }

    /**
     * Returns the start-of-day of the mocked local date in the system
     * default time zone as a {@link Date}.
     */
    public static Date mockDate() {
        return toStartOfDay(mockDateTime());
    }

    /**
     * Returns the local time-of-day portion of the mocked instant,
     * carried on the epoch-day-0 date.
     */
    public static Date mockTime() {
        return toTimeOfDay(mockDateTime());
    }

    /* ----------------------- of (timestamp -> date parts) ----------------------- */

    /**
     * Converts an epoch millis to a {@link Date}.
     *
     * @param epochMillis the milliseconds since 1970-01-01T00:00:00Z
     * @return the corresponding {@link Date}
     */
    public static Date ofDateTime(long epochMillis) {
        return new Date(epochMillis);
    }

    /**
     * Converts an epoch millis to the start-of-day of the corresponding
     * local date in the system default time zone.
     *
     * @param epochMillis the milliseconds since 1970-01-01T00:00:00Z
     * @return the start-of-day as a {@link Date}
     */
    public static Date ofDate(long epochMillis) {
        return toStartOfDay(new Date(epochMillis));
    }

    /**
     * Converts an epoch millis to the local time-of-day portion, carried
     * on the epoch-day-0 date.
     *
     * @param epochMillis the milliseconds since 1970-01-01T00:00:00Z
     * @return the local time-of-day as a {@link Date} on epoch-day-0
     */
    public static Date ofTime(long epochMillis) {
        return toTimeOfDay(new Date(epochMillis));
    }

    /* ----------------------- parse (string -> Date) ----------------------- */

    /**
     * Parses a {@code yyyy-MM-dd HH:mm:ss} string into a {@link Date}
     * interpreted in the system default time zone.
     *
     * @param str the date-time string to parse
     * @return the parsed {@link Date}
     */
    public static Date parse(String str) {
        return toDate(HighDate.parse(str));
    }

    /**
     * Parses a {@code yyyy-MM-dd} string into a {@link Date} at
     * start-of-day in the system default time zone.
     *
     * @param str the date string to parse
     * @return the parsed {@link Date}
     */
    public static Date parseDate(String str) {
        return toDate(HighDate.parseDate(str));
    }

    /**
     * Parses a {@code HH:mm:ss} string into a {@link Date} carried on
     * the epoch-day-0 date.
     * <p>
     * Note: This is a compatibility wrapper for "pure time"; new code
     * should use {@link LocalTime} directly.
     *
     * @param str the time string to parse
     * @return the parsed {@link Date} on epoch-day-0
     */
    public static Date parseTime(String str) {
        return toDate(HighDate.parseTime(str));
    }

    /* ----------------------- format (Date -> string) ----------------------- */

    /**
     * Formats a {@link Date} to {@code yyyy-MM-dd HH:mm:ss} in the
     * system default time zone.
     *
     * @param date the date to format
     * @return the formatted string
     */
    public static String formatDateTime(Date date) {
        return HighDate.format(HighDate.ofDateTime(date.toInstant()));
    }

    /**
     * Formats a {@link Date} to {@code yyyy-MM-dd} in the system default
     * time zone.
     *
     * @param date the date to format
     * @return the formatted date string
     */
    public static String formatDate(Date date) {
        return HighDate.format(HighDate.ofDate(date.toInstant()));
    }

    /**
     * Formats a {@link Date} to {@code HH:mm:ss} in the system default
     * time zone.
     *
     * @param date the date to format
     * @return the formatted time string
     */
    public static String formatTime(Date date) {
        return HighDate.format(HighDate.ofTime(date.toInstant()));
    }

    /* ----------------------- epoch ----------------------- */

    /**
     * Converts a {@link Date} to epoch seconds.
     *
     * @param date the date to convert
     * @return the epoch seconds
     */
    public static long toEpochSeconds(Date date) {
        return date.toInstant().getEpochSecond();
    }

    /**
     * Converts a {@link Date} to epoch millis.
     *
     * @param date the date to convert
     * @return the epoch milliseconds
     */
    public static long toEpochMillis(Date date) {
        return date.getTime();
    }

    /* ----------------------- day boundary (system default zone) ----------------------- */

    /**
     * Returns the start-of-day (00:00:00) of the given date-time in the
     * system default time zone.
     *
     * @param dateTime the source date-time
     * @return the start-of-day as a {@link Date}
     */
    public static Date toStartOfDay(Date dateTime) {
        return toDate(HighDate.toStartOfDay(dateTime.toInstant()));
    }

    /**
     * Returns the start of the next day (00:00:00) in the system default
     * time zone.
     * <p>
     * Recommended for day-range queries using the half-open interval
     * {@code [startOfDay, startOfNextDay)} to avoid precision ambiguity
     * with end-of-day.
     *
     * @param dateTime the source date-time
     * @return the start of the next day as a {@link Date}
     */
    public static Date toStartOfNextDay(Date dateTime) {
        return toDate(HighDate.toStartOfNextDay(dateTime.toInstant()));
    }

    /**
     * Returns the start of the current month (first day 00:00:00) in the
     * system default time zone.
     *
     * @param dateTime the source date-time
     * @return the start of the month as a {@link Date}
     */
    public static Date toStartOfMonth(Date dateTime) {
        return toDate(HighDate.toStartOfMonth(dateTime.toInstant()));
    }

    /**
     * Returns the start of the next month (first day 00:00:00) in the
     * system default time zone.
     *
     * @param dateTime the source date-time
     * @return the start of the next month as a {@link Date}
     */
    public static Date toStartOfNextMonth(Date dateTime) {
        return toDate(HighDate.toStartOfNextMonth(dateTime.toInstant()));
    }

    /**
     * Returns the start of the current year (January 1st 00:00:00) in the
     * system default time zone.
     *
     * @param dateTime the source date-time
     * @return the start of the year as a {@link Date}
     */
    public static Date toStartOfYear(Date dateTime) {
        return toDate(HighDate.toStartOfYear(dateTime.toInstant()));
    }

    /**
     * Returns the start of the next year (January 1st 00:00:00) in the
     * system default time zone.
     *
     * @param dateTime the source date-time
     * @return the start of the next year as a {@link Date}
     */
    public static Date toStartOfNextYear(Date dateTime) {
        return toDate(HighDate.toStartOfNextYear(dateTime.toInstant()));
    }

    /**
     * Extracts the local time-of-day portion (hour:minute:second:nanos)
     * and returns it as a {@link Date} on the epoch-day-0 base date.
     * <p>
     * Note: {@link Date} is not suited for "pure time"; this exists only
     * for legacy API compatibility.
     *
     * @param dateTime the source date-time
     * @return the local time-of-day as a {@link Date} on epoch-day-0
     */
    public static Date toTimeOfDay(Date dateTime) {
        LocalTime lt = HighDate.ofTime(dateTime.toInstant());
        LocalDate base = LocalDate.ofEpochDay(0);
        return Date.from(LocalDateTime.of(base, lt).atZone(zone()).toInstant());
    }

    /* ----------------------- timezone parsing ----------------------- */

    /**
     * Parses a time zone ID string into a {@link TimeZone}.
     * <p>
     * Unlike {@link TimeZone#getTimeZone(String)}, this method throws an
     * exception for invalid IDs instead of silently falling back to GMT.
     * <p>
     * Example: {@code LowDate.parseTimeZone("Asia/Shanghai")}.
     *
     * @param timeZoneId the time zone ID (e.g. "Asia/Shanghai")
     * @return the resolved {@link TimeZone}
     * @throws SysException if the ID is null, blank, or invalid
     */
    public static TimeZone parseTimeZone(String timeZoneId) {
        if (timeZoneId == null || timeZoneId.isBlank()) {
            throw new SysException("TimeZone ID must not be null or blank", BaseError.SERVICE_ERROR);
        }

        String id = timeZoneId.trim();
        TimeZone tz = TimeZone.getTimeZone(id);

        // TimeZone.getTimeZone silently returns GMT for invalid IDs
        if (!tz.getID().equals(id) && !"GMT".equalsIgnoreCase(id)) {
            throw new SysException("Invalid TimeZone ID: " + timeZoneId, BaseError.SERVICE_ERROR);
        }

        return tz;
    }

    /**
     * Converts an {@link Instant} to a {@link Date}.
     *
     * @param instant the instant to convert, may be null
     * @return the corresponding {@link Date}, or null if input is null
     */
    public static Date toDate(Instant instant) {
        if (instant == null) {
            return null;
        }
        return Date.from(instant);
    }

    /**
     * Converts a {@link LocalDateTime} to a {@link Date} using the
     * system default time zone.
     *
     * @param dateTime the local date-time to convert
     * @return the corresponding {@link Date}
     */
    public static Date toDate(LocalDateTime dateTime) {
        return toDate(HighDate.toInstant(dateTime));
    }

    /**
     * Converts a {@link LocalDate} to a {@link Date} at start-of-day
     * in the system default time zone.
     *
     * @param date the local date to convert
     * @return the corresponding {@link Date}
     */
    public static Date toDate(LocalDate date) {
        return toDate(HighDate.toInstant(date));
    }

    /**
     * Converts a {@link LocalTime} to a compatibility {@link Date} on
     * the epoch-day-0 base date.
     *
     * @param time the local time to convert
     * @return the corresponding {@link Date} on epoch-day-0
     */
    public static Date toDate(LocalTime time) {
        if (time == null) {
            return null;
        }
        return toDate(LocalDateTime.of(LocalDate.ofEpochDay(0), time));
    }

    /**
     * Converts a {@link Date} back to an {@link Instant}.
     *
     * @param date the date to convert, may be null
     * @return the corresponding {@link Instant}, or null if input is null
     */
    public static Instant toInstant(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant();
    }

    /**
     * Converts a {@link Date} to a {@link LocalDateTime} in the system
     * default time zone.
     *
     * @param date the date to convert, may be null
     * @return the corresponding {@link LocalDateTime}, or null if input is null
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return HighDate.ofDateTime(date.toInstant());
    }

    /**
     * Converts a {@link Date} to a {@link LocalDate} in the system
     * default time zone.
     *
     * @param date the date to convert, may be null
     * @return the corresponding {@link LocalDate}, or null if input is null
     */
    public static LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return HighDate.ofDate(date.toInstant());
    }

    /**
     * Converts a {@link Date} to a {@link LocalTime} in the system
     * default time zone.
     *
     * @param date the date to convert, may be null
     * @return the corresponding {@link LocalTime}, or null if input is null
     */
    public static LocalTime toLocalTime(Date date) {
        if (date == null) {
            return null;
        }
        return HighDate.ofTime(date.toInstant());
    }

    /**
     * Formats a {@link Date} using a custom pattern in the system default
     * time zone.
     *
     * @param date    the date to format
     * @param pattern the {@link java.time.format.DateTimeFormatter} pattern
     * @return the formatted string
     */
    public static String formatDateTime(Date date, String pattern) {
        return HighDate.format(toLocalDateTime(date), pattern);
    }

    /**
     * Formats the date portion of a {@link Date} using a custom pattern.
     *
     * @param date    the date to format
     * @param pattern the {@link java.time.format.DateTimeFormatter} pattern
     * @return the formatted date string
     */
    public static String formatDate(Date date, String pattern) {
        return HighDate.format(toLocalDate(date), pattern);
    }

    /**
     * Formats the time portion of a {@link Date} using a custom pattern.
     *
     * @param date    the date to format
     * @param pattern the {@link java.time.format.DateTimeFormatter} pattern
     * @return the formatted time string
     */
    public static String formatTime(Date date, String pattern) {
        return HighDate.format(toLocalTime(date), pattern);
    }

    /**
     * Parses a date-time string using a custom pattern into a {@link Date}.
     *
     * @param str     the date-time string to parse
     * @param pattern the {@link java.time.format.DateTimeFormatter} pattern
     * @return the parsed {@link Date}
     */
    public static Date parse(String str, String pattern) {
        return toDate(HighDate.parse(str, pattern));
    }

    /**
     * Parses a date string using a custom pattern into a {@link Date}
     * at start-of-day.
     *
     * @param str     the date string to parse
     * @param pattern the {@link java.time.format.DateTimeFormatter} pattern
     * @return the parsed {@link Date}
     */
    public static Date parseDate(String str, String pattern) {
        return toDate(HighDate.parseDate(str, pattern));
    }

    /**
     * Parses a time string using a custom pattern into a {@link Date}
     * on epoch-day-0.
     *
     * @param str     the time string to parse
     * @param pattern the {@link java.time.format.DateTimeFormatter} pattern
     * @return the parsed {@link Date} on epoch-day-0
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
