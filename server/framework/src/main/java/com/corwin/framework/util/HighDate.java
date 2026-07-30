package com.corwin.framework.util;

import com.corwin.framework.config.DefaultConfigKeys;
import com.corwin.framework.config.ConfigRegistry;
import com.corwin.framework.constant.DateTimePatterns;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Primary date/time utility based on {@code java.time}.
 * <p>
 * Core conventions:
 * <ul>
 *   <li>The timeline is represented by {@link Instant} — suitable for
 *       storage, comparison, and transmission</li>
 *   <li>Display and input use {@link LocalDateTime} / {@link LocalDate} /
 *       {@link LocalTime}</li>
 *   <li>When no time zone is explicitly specified, the system default
 *       ({@link ZoneId#systemDefault()}) is used</li>
 *   <li>Day-range queries should use the half-open interval
 *       {@code [startInclusive, endExclusive)}</li>
 * </ul>
 *
 * <pre>{@code
 * Instant now = HighDate.mockInstant();
 * Instant dayStart = HighDate.toStartOfDay(now);
 * Instant nextDayStart = HighDate.toStartOfNextDay(now);
 * // Query: createdAt >= dayStart && createdAt < nextDayStart
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
     * Returns the system default time zone.
     * <p>
     * All code should use this single entry point instead of calling
     * {@code ZoneId.systemDefault()} directly, to keep a central point
     * for potential overrides.
     *
     * @return the system default {@link ZoneId}
     */
    public static ZoneId zone() {
        return ZoneId.systemDefault();
    }

    /* ----------------------- real (actual current time) ----------------------- */

    /**
     * Returns the current instant on the timeline.
     * Does not depend on time zone.
     *
     * @return the current {@link Instant}
     */
    public static Instant realInstant() {
        return Instant.now();
    }

    /**
     * Returns the current local date-time in the system default time zone.
     *
     * @return the current {@link LocalDateTime}
     */
    public static LocalDateTime realDateTime() {
        return ofDateTime(realInstant());
    }

    /**
     * Returns the current local date in the system default time zone.
     *
     * @return the current {@link LocalDate}
     */
    public static LocalDate realDate() {
        return realDateTime().toLocalDate();
    }

    /**
     * Returns the current local time in the system default time zone.
     *
     * @return the current {@link LocalTime}
     */
    public static LocalTime realTime() {
        return realDateTime().toLocalTime();
    }

    /**
     * Returns the current timestamp in milliseconds.
     *
     * @return the current epoch millis
     */
    public static long realTimestampMillis() {
        return realInstant().toEpochMilli();
    }

    /* ----------------------- mock (offset current time) ----------------------- */

    /**
     * Returns the mocked current instant (= now + offsetSeconds).
     * Does not depend on time zone. The offset is read from
     * {@link DefaultConfigKeys#TIME_OFFSET}.
     *
     * @return the mocked {@link Instant}
     */
    public static Instant mockInstant() {
        long offsetSeconds = ConfigRegistry.longV(DefaultConfigKeys.TIME_OFFSET);
        return Instant.now().plusSeconds(offsetSeconds);
    }

    /**
     * Returns the mocked local date-time in the system default time zone.
     *
     * @return the mocked {@link LocalDateTime}
     */
    public static LocalDateTime mockDateTime() {
        return ofDateTime(mockInstant());
    }

    /**
     * Returns the mocked local date in the system default time zone.
     *
     * @return the mocked {@link LocalDate}
     */
    public static LocalDate mockDate() {
        return mockDateTime().toLocalDate();
    }

    /**
     * Returns the mocked local time in the system default time zone.
     *
     * @return the mocked {@link LocalTime}
     */
    public static LocalTime mockTime() {
        return mockDateTime().toLocalTime();
    }

    /**
     * Returns the mocked timestamp in milliseconds.
     *
     * @return the mocked epoch millis
     */
    public static long mockTimestampMillis() {
        return mockInstant().toEpochMilli();
    }

    /* ----------------------- convert (timestamp <-> local) ----------------------- */

    /**
     * Creates an {@link Instant} from epoch milliseconds.
     *
     * @param epochMillis the milliseconds since 1970-01-01T00:00:00Z
     * @return the corresponding {@link Instant}
     */
    public static Instant ofInstant(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis);
    }

    /**
     * Creates an {@link Instant} from epoch seconds.
     *
     * @param epochSeconds the seconds since 1970-01-01T00:00:00Z
     * @return the corresponding {@link Instant}
     */
    public static Instant ofInstantSeconds(long epochSeconds) {
        return Instant.ofEpochSecond(epochSeconds);
    }

    /**
     * Converts epoch milliseconds to a {@link LocalDateTime} in the
     * system default time zone.
     *
     * @param epochMillis the milliseconds since 1970-01-01T00:00:00Z
     * @return the corresponding {@link LocalDateTime}
     */
    public static LocalDateTime ofDateTime(long epochMillis) {
        return ofDateTime(ofInstant(epochMillis));
    }

    /**
     * Converts an {@link Instant} to a {@link LocalDateTime} in the
     * system default time zone.
     *
     * @param instant the instant to convert
     * @return the corresponding {@link LocalDateTime}
     */
    public static LocalDateTime ofDateTime(Instant instant) {
        return ofDateTime(instant, zone());
    }

    /**
     * Converts an {@link Instant} to a {@link LocalDateTime} in the
     * specified time zone.
     *
     * @param instant the instant to convert
     * @param zoneId  the target time zone
     * @return the corresponding {@link LocalDateTime}
     */
    public static LocalDateTime ofDateTime(Instant instant, ZoneId zoneId) {
        return LocalDateTime.ofInstant(instant, zoneId);
    }

    /**
     * Converts epoch milliseconds to a {@link LocalDate} in the system
     * default time zone.
     *
     * @param epochMillis the milliseconds since 1970-01-01T00:00:00Z
     * @return the corresponding {@link LocalDate}
     */
    public static LocalDate ofDate(long epochMillis) {
        return ofDateTime(epochMillis).toLocalDate();
    }

    /**
     * Converts an {@link Instant} to a {@link LocalDate} in the system
     * default time zone.
     *
     * @param instant the instant to convert
     * @return the corresponding {@link LocalDate}
     */
    public static LocalDate ofDate(Instant instant) {
        return ofDateTime(instant).toLocalDate();
    }

    /**
     * Converts an {@link Instant} to a {@link LocalDate} in the specified
     * time zone.
     *
     * @param instant the instant to convert
     * @param zoneId  the target time zone
     * @return the corresponding {@link LocalDate}
     */
    public static LocalDate ofDate(Instant instant, ZoneId zoneId) {
        return ofDateTime(instant, zoneId).toLocalDate();
    }

    /**
     * Converts epoch milliseconds to a {@link LocalTime} in the system
     * default time zone.
     *
     * @param epochMillis the milliseconds since 1970-01-01T00:00:00Z
     * @return the corresponding {@link LocalTime}
     */
    public static LocalTime ofTime(long epochMillis) {
        return ofDateTime(epochMillis).toLocalTime();
    }

    /**
     * Converts an {@link Instant} to a {@link LocalTime} in the system
     * default time zone.
     *
     * @param instant the instant to convert
     * @return the corresponding {@link LocalTime}
     */
    public static LocalTime ofTime(Instant instant) {
        return ofDateTime(instant).toLocalTime();
    }

    /**
     * Converts an {@link Instant} to a {@link LocalTime} in the specified
     * time zone.
     *
     * @param instant the instant to convert
     * @param zoneId  the target time zone
     * @return the corresponding {@link LocalTime}
     */
    public static LocalTime ofTime(Instant instant, ZoneId zoneId) {
        return ofDateTime(instant, zoneId).toLocalTime();
    }

    /**
     * Converts an {@link Instant} to a {@link ZonedDateTime} in the
     * system default time zone.
     *
     * @param instant the instant to convert
     * @return the corresponding {@link ZonedDateTime}
     */
    public static ZonedDateTime ofZonedDateTime(Instant instant) {
        return instant.atZone(zone());
    }

    /**
     * Converts an {@link Instant} to a {@link ZonedDateTime} in the
     * specified time zone.
     *
     * @param instant the instant to convert
     * @param zoneId  the target time zone
     * @return the corresponding {@link ZonedDateTime}
     */
    public static ZonedDateTime ofZonedDateTime(Instant instant, ZoneId zoneId) {
        return instant.atZone(zoneId);
    }

    /**
     * Converts a {@link LocalDateTime} to an {@link Instant} using the
     * system default time zone.
     *
     * @param dateTime the local date-time to convert, may be null
     * @return the corresponding {@link Instant}, or null if input is null
     */
    public static Instant toInstant(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(zone()).toInstant();
    }

    /**
     * Converts a {@link LocalDateTime} to an {@link Instant} using the
     * specified time zone.
     *
     * @param dateTime the local date-time to convert, may be null
     * @param zoneId   the target time zone
     * @return the corresponding {@link Instant}, or null if input is null
     */
    public static Instant toInstant(LocalDateTime dateTime, ZoneId zoneId) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(zoneId).toInstant();
    }

    /**
     * Converts a {@link LocalDate} to an {@link Instant} at start-of-day
     * in the system default time zone.
     *
     * @param date the local date to convert, may be null
     * @return the corresponding {@link Instant}, or null if input is null
     */
    public static Instant toInstant(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atStartOfDay(zone()).toInstant();
    }

    /**
     * Converts a {@link LocalDate} to an {@link Instant} at start-of-day
     * in the specified time zone.
     *
     * @param date   the local date to convert, may be null
     * @param zoneId the target time zone
     * @return the corresponding {@link Instant}, or null if input is null
     */
    public static Instant toInstant(LocalDate date, ZoneId zoneId) {
        if (date == null) {
            return null;
        }
        return date.atStartOfDay(zoneId).toInstant();
    }

    /**
     * Combines a {@link LocalDate} and {@link LocalTime} into an
     * {@link Instant} using the system default time zone.
     *
     * @param date the date portion, may be null
     * @param time the time portion, may be null
     * @return the combined {@link Instant}, or null if either input is null
     */
    public static Instant toInstant(LocalDate date, LocalTime time) {
        return toInstant(date, time, zone());
    }

    /**
     * Combines a {@link LocalDate} and {@link LocalTime} into an
     * {@link Instant} using the specified time zone.
     * <p>
     * Useful for form inputs that split date and time into separate fields.
     *
     * @param date   the date portion, may be null
     * @param time   the time portion, may be null
     * @param zoneId the target time zone
     * @return the combined {@link Instant}, or null if either input is null
     */
    public static Instant toInstant(LocalDate date, LocalTime time, ZoneId zoneId) {
        if (date == null || time == null) {
            return null;
        }
        return LocalDateTime.of(date, time).atZone(zoneId).toInstant();
    }

    /**
     * Converts a {@link LocalDateTime} to epoch milliseconds using the
     * system default time zone.
     *
     * @param ldt the local date-time to convert, may be null
     * @return the epoch millis, or 0 if input is null
     */
    public static long toEpochMillis(LocalDateTime ldt) {
        if (ldt == null) {
            return 0L;
        }
        return toInstant(ldt).toEpochMilli();
    }

    /**
     * Converts a {@link LocalDateTime} to epoch seconds using the system
     * default time zone.
     *
     * @param ldt the local date-time to convert, may be null
     * @return the epoch seconds, or 0 if input is null
     */
    public static long toEpochSeconds(LocalDateTime ldt) {
        if (ldt == null) {
            return 0L;
        }
        return toInstant(ldt).getEpochSecond();
    }

    /**
     * Converts an {@link Instant} to epoch milliseconds.
     *
     * @param instant the instant to convert, may be null
     * @return the epoch millis, or 0 if input is null
     */
    public static long toEpochMillis(Instant instant) {
        if (instant == null) {
            return 0L;
        }
        return instant.toEpochMilli();
    }

    /**
     * Converts an {@link Instant} to epoch seconds.
     *
     * @param instant the instant to convert, may be null
     * @return the epoch seconds, or 0 if input is null
     */
    public static long toEpochSeconds(Instant instant) {
        if (instant == null) {
            return 0L;
        }
        return instant.getEpochSecond();
    }

    /* ----------------------- range / boundary ----------------------- */

    /**
     * Returns the start of the day ({@code 00:00:00}) for the given
     * date-time.
     *
     * @param dateTime the source date-time, may be null
     * @return the start-of-day, or null if input is null
     */
    public static LocalDateTime toStartOfDay(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalDate().atStartOfDay();
    }

    /**
     * Returns the start of the day ({@code 00:00:00}) for the given date.
     *
     * @param date the source date, may be null
     * @return the start-of-day, or null if input is null
     */
    public static LocalDateTime toStartOfDay(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atStartOfDay();
    }

    /**
     * Returns the start of the next day ({@code 00:00:00}) for the given
     * date-time.
     * <p>
     * Recommended as the exclusive upper bound in day-range queries.
     *
     * @param dateTime the source date-time, may be null
     * @return the start of the next day, or null if input is null
     */
    public static LocalDateTime toStartOfNextDay(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalDate().plusDays(1).atStartOfDay();
    }

    /**
     * Returns the start of the next day ({@code 00:00:00}) for the given date.
     *
     * @param date the source date, may be null
     * @return the start of the next day, or null if input is null
     */
    public static LocalDateTime toStartOfNextDay(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.plusDays(1).atStartOfDay();
    }

    /**
     * Returns the last nanosecond of the day for the given date-time.
     * <p>
     * Exists for "end-of-day" compatibility. For query boundaries,
     * prefer {@link #toStartOfNextDay(LocalDateTime)}.
     *
     * @param dateTime the source date-time, may be null
     * @return the end-of-day, or null if input is null
     */
    public static LocalDateTime toEndOfDay(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return toStartOfNextDay(dateTime).minusNanos(1);
    }

    /**
     * Returns the last nanosecond of the day for the given date.
     *
     * @param date the source date, may be null
     * @return the end-of-day, or null if input is null
     */
    public static LocalDateTime toEndOfDay(LocalDate date) {
        if (date == null) {
            return null;
        }
        return toStartOfNextDay(date).minusNanos(1);
    }

    /**
     * Normalizes an {@link Instant} to the start of its day in the
     * system default time zone.
     *
     * @param instant the source instant, may be null
     * @return the start-of-day instant, or null if input is null
     */
    public static Instant toStartOfDay(Instant instant) {
        return toStartOfDay(instant, zone());
    }

    /**
     * Normalizes an {@link Instant} to the start of its day in the
     * specified time zone.
     *
     * @param instant the source instant, may be null
     * @param zoneId  the target time zone
     * @return the start-of-day instant, or null if input is null
     */
    public static Instant toStartOfDay(Instant instant, ZoneId zoneId) {
        if (instant == null) {
            return null;
        }
        return ofDate(instant, zoneId).atStartOfDay(zoneId).toInstant();
    }

    /**
     * Normalizes an {@link Instant} to the start of the next day in the
     * system default time zone.
     *
     * @param instant the source instant, may be null
     * @return the next-day-start instant, or null if input is null
     */
    public static Instant toStartOfNextDay(Instant instant) {
        return toStartOfNextDay(instant, zone());
    }

    /**
     * Normalizes an {@link Instant} to the start of the next day in the
     * specified time zone.
     *
     * @param instant the source instant, may be null
     * @param zoneId  the target time zone
     * @return the next-day-start instant, or null if input is null
     */
    public static Instant toStartOfNextDay(Instant instant, ZoneId zoneId) {
        if (instant == null) {
            return null;
        }
        return ofDate(instant, zoneId).plusDays(1).atStartOfDay(zoneId).toInstant();
    }

    /**
     * Returns the last nanosecond of the day for the given instant in
     * the system default time zone.
     *
     * @param instant the source instant, may be null
     * @return the end-of-day instant, or null if input is null
     */
    public static Instant toEndOfDay(Instant instant) {
        return toEndOfDay(instant, zone());
    }

    /**
     * Returns the last nanosecond of the day for the given instant in
     * the specified time zone.
     *
     * @param instant the source instant, may be null
     * @param zoneId  the target time zone
     * @return the end-of-day instant, or null if input is null
     */
    public static Instant toEndOfDay(Instant instant, ZoneId zoneId) {
        Instant nextDayStart = toStartOfNextDay(instant, zoneId);
        if (nextDayStart == null) {
            return null;
        }
        return nextDayStart.minusNanos(1);
    }

    /**
     * Returns the start of the current month ({@code 1st 00:00:00}) for
     * the given date-time.
     *
     * @param dateTime the source date-time, may be null
     * @return the month-start, or null if input is null
     */
    public static LocalDateTime toStartOfMonth(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalDate().withDayOfMonth(1).atStartOfDay();
    }

    /**
     * Returns the start of the next month ({@code 1st 00:00:00}) for
     * the given date-time.
     *
     * @param dateTime the source date-time, may be null
     * @return the next-month-start, or null if input is null
     */
    public static LocalDateTime toStartOfNextMonth(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalDate().withDayOfMonth(1).plusMonths(1).atStartOfDay();
    }

    /**
     * Returns the start of the current year ({@code Jan 1st 00:00:00})
     * for the given date-time.
     *
     * @param dateTime the source date-time, may be null
     * @return the year-start, or null if input is null
     */
    public static LocalDateTime toStartOfYear(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalDate().withDayOfYear(1).atStartOfDay();
    }

    /**
     * Returns the start of the next year ({@code Jan 1st 00:00:00})
     * for the given date-time.
     *
     * @param dateTime the source date-time, may be null
     * @return the next-year-start, or null if input is null
     */
    public static LocalDateTime toStartOfNextYear(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalDate().withDayOfYear(1).plusYears(1).atStartOfDay();
    }

    /**
     * Normalizes an {@link Instant} to the start of its month in the
     * system default time zone.
     *
     * @param instant the source instant, may be null
     * @return the month-start instant, or null if input is null
     */
    public static Instant toStartOfMonth(Instant instant) {
        return toStartOfMonth(instant, zone());
    }

    /**
     * Normalizes an {@link Instant} to the start of its month in the
     * specified time zone.
     *
     * @param instant the source instant, may be null
     * @param zoneId  the target time zone
     * @return the month-start instant, or null if input is null
     */
    public static Instant toStartOfMonth(Instant instant, ZoneId zoneId) {
        if (instant == null) {
            return null;
        }
        LocalDate date = ofDate(instant, zoneId).withDayOfMonth(1);
        return date.atStartOfDay(zoneId).toInstant();
    }

    /**
     * Normalizes an {@link Instant} to the start of the next month in the
     * system default time zone.
     *
     * @param instant the source instant, may be null
     * @return the next-month-start instant, or null if input is null
     */
    public static Instant toStartOfNextMonth(Instant instant) {
        return toStartOfNextMonth(instant, zone());
    }

    /**
     * Normalizes an {@link Instant} to the start of the next month in the
     * specified time zone.
     *
     * @param instant the source instant, may be null
     * @param zoneId  the target time zone
     * @return the next-month-start instant, or null if input is null
     */
    public static Instant toStartOfNextMonth(Instant instant, ZoneId zoneId) {
        if (instant == null) {
            return null;
        }
        LocalDate date = ofDate(instant, zoneId).withDayOfMonth(1).plusMonths(1);
        return date.atStartOfDay(zoneId).toInstant();
    }

    /**
     * Normalizes an {@link Instant} to the start of its year in the
     * system default time zone.
     *
     * @param instant the source instant, may be null
     * @return the year-start instant, or null if input is null
     */
    public static Instant toStartOfYear(Instant instant) {
        return toStartOfYear(instant, zone());
    }

    /**
     * Normalizes an {@link Instant} to the start of its year in the
     * specified time zone.
     *
     * @param instant the source instant, may be null
     * @param zoneId  the target time zone
     * @return the year-start instant, or null if input is null
     */
    public static Instant toStartOfYear(Instant instant, ZoneId zoneId) {
        if (instant == null) {
            return null;
        }
        LocalDate date = ofDate(instant, zoneId).withDayOfYear(1);
        return date.atStartOfDay(zoneId).toInstant();
    }

    /**
     * Normalizes an {@link Instant} to the start of the next year in the
     * system default time zone.
     *
     * @param instant the source instant, may be null
     * @return the next-year-start instant, or null if input is null
     */
    public static Instant toStartOfNextYear(Instant instant) {
        return toStartOfNextYear(instant, zone());
    }

    /**
     * Normalizes an {@link Instant} to the start of the next year in the
     * specified time zone.
     *
     * @param instant the source instant, may be null
     * @param zoneId  the target time zone
     * @return the next-year-start instant, or null if input is null
     */
    public static Instant toStartOfNextYear(Instant instant, ZoneId zoneId) {
        if (instant == null) {
            return null;
        }
        LocalDate date = ofDate(instant, zoneId).withDayOfYear(1).plusYears(1);
        return date.atStartOfDay(zoneId).toInstant();
    }

    /**
     * Computes the difference in milliseconds for the half-open interval
     * {@code [startInclusive, endExclusive)}.
     *
     * @param startInclusive the start of the interval (inclusive)
     * @param endExclusive   the end of the interval (exclusive)
     * @return the number of milliseconds between the two instants
     * @throws NullPointerException if either argument is null
     */
    public static long betweenMillis(Instant startInclusive, Instant endExclusive) {
        Objects.requireNonNull(startInclusive, "startInclusive required");
        Objects.requireNonNull(endExclusive, "endExclusive required");
        return endExclusive.toEpochMilli() - startInclusive.toEpochMilli();
    }

    /**
     * Computes the difference in seconds for the half-open interval
     * {@code [startInclusive, endExclusive)}.
     *
     * @param startInclusive the start of the interval (inclusive)
     * @param endExclusive   the end of the interval (exclusive)
     * @return the number of seconds between the two instants
     * @throws NullPointerException if either argument is null
     */
    public static long betweenSeconds(Instant startInclusive, Instant endExclusive) {
        Objects.requireNonNull(startInclusive, "startInclusive required");
        Objects.requireNonNull(endExclusive, "endExclusive required");
        return endExclusive.getEpochSecond() - startInclusive.getEpochSecond();
    }

    /**
     * Returns the earlier of the two instants; either argument may be
     * {@code null}.
     *
     * @param left  the first instant, may be null
     * @param right the second instant, may be null
     * @return the earlier instant, or the non-null one if the other is null
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
     * Returns the later of the two instants; either argument may be
     * {@code null}.
     *
     * @param left  the first instant, may be null
     * @param right the second instant, may be null
     * @return the later instant, or the non-null one if the other is null
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

    /**
     * Parses a date-time string using the default pattern
     * {@code yyyy-MM-dd HH:mm:ss}.
     *
     * @param str the string to parse
     * @return the parsed {@link LocalDateTime}
     */
    public static LocalDateTime parse(String str) {
        return parse(str, DateTimePatterns.DATE_TIME_FMT);
    }

    /**
     * Parses a date string using the default pattern {@code yyyy-MM-dd}.
     *
     * @param str the string to parse
     * @return the parsed {@link LocalDate}
     */
    public static LocalDate parseDate(String str) {
        return parseDate(str, DateTimePatterns.DATE_FMT);
    }

    /**
     * Parses a time string using the default pattern {@code HH:mm:ss}.
     *
     * @param str the string to parse
     * @return the parsed {@link LocalTime}
     */
    public static LocalTime parseTime(String str) {
        return parseTime(str, DateTimePatterns.TIME_FMT);
    }

    /**
     * Parses a date-time string using a custom {@link DateTimeFormatter}.
     *
     * @param str       the string to parse
     * @param formatter the formatter to use
     * @return the parsed {@link LocalDateTime}
     */
    public static LocalDateTime parse(String str, DateTimeFormatter formatter) {
        return LocalDateTime.parse(str, formatter);
    }

    /**
     * Parses a date-time string using a custom pattern.
     * <p>
     * Example: {@code HighDate.parse("2026/03/14 08:30", "yyyy/MM/dd HH:mm")}.
     *
     * @param str     the string to parse
     * @param pattern the pattern to use
     * @return the parsed {@link LocalDateTime}
     */
    public static LocalDateTime parse(String str, String pattern) {
        return parse(str, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * Parses a date string using a custom {@link DateTimeFormatter}.
     *
     * @param str       the string to parse
     * @param formatter the formatter to use
     * @return the parsed {@link LocalDate}
     */
    public static LocalDate parseDate(String str, DateTimeFormatter formatter) {
        return LocalDate.parse(str, formatter);
    }

    /**
     * Parses a date string using a custom pattern.
     *
     * @param str     the string to parse
     * @param pattern the pattern to use
     * @return the parsed {@link LocalDate}
     */
    public static LocalDate parseDate(String str, String pattern) {
        return parseDate(str, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * Parses a time string using a custom {@link DateTimeFormatter}.
     *
     * @param str       the string to parse
     * @param formatter the formatter to use
     * @return the parsed {@link LocalTime}
     */
    public static LocalTime parseTime(String str, DateTimeFormatter formatter) {
        return LocalTime.parse(str, formatter);
    }

    /**
     * Parses a time string using a custom pattern.
     *
     * @param str     the string to parse
     * @param pattern the pattern to use
     * @return the parsed {@link LocalTime}
     */
    public static LocalTime parseTime(String str, String pattern) {
        return parseTime(str, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * Formats a {@link LocalDateTime} using the default pattern
     * {@code yyyy-MM-dd HH:mm:ss}.
     *
     * @param ldt the date-time to format
     * @return the formatted string
     */
    public static String format(LocalDateTime ldt) {
        return format(ldt, DateTimePatterns.DATE_TIME_FMT);
    }

    /**
     * Formats a {@link LocalDate} using the default pattern
     * {@code yyyy-MM-dd}.
     *
     * @param ld the date to format
     * @return the formatted string
     */
    public static String format(LocalDate ld) {
        return format(ld, DateTimePatterns.DATE_FMT);
    }

    /**
     * Formats a {@link LocalTime} using the default pattern
     * {@code HH:mm:ss}.
     *
     * @param lt the time to format
     * @return the formatted string
     */
    public static String format(LocalTime lt) {
        return format(lt, DateTimePatterns.TIME_FMT);
    }

    /**
     * Formats a {@link LocalDateTime} using a custom
     * {@link DateTimeFormatter}.
     *
     * @param ldt       the date-time to format
     * @param formatter the formatter to use
     * @return the formatted string
     */
    public static String format(LocalDateTime ldt, DateTimeFormatter formatter) {
        return ldt.format(formatter);
    }

    /**
     * Formats a {@link LocalDateTime} using a custom pattern.
     *
     * @param ldt     the date-time to format
     * @param pattern the pattern to use
     * @return the formatted string
     */
    public static String format(LocalDateTime ldt, String pattern) {
        return format(ldt, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * Formats a {@link LocalDate} using a custom
     * {@link DateTimeFormatter}.
     *
     * @param ld        the date to format
     * @param formatter the formatter to use
     * @return the formatted string
     */
    public static String format(LocalDate ld, DateTimeFormatter formatter) {
        return ld.format(formatter);
    }

    /**
     * Formats a {@link LocalDate} using a custom pattern.
     *
     * @param ld     the date to format
     * @param pattern the pattern to use
     * @return the formatted string
     */
    public static String format(LocalDate ld, String pattern) {
        return format(ld, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * Formats a {@link LocalTime} using a custom
     * {@link DateTimeFormatter}.
     *
     * @param lt        the time to format
     * @param formatter the formatter to use
     * @return the formatted string
     */
    public static String format(LocalTime lt, DateTimeFormatter formatter) {
        return lt.format(formatter);
    }

    /**
     * Formats a {@link LocalTime} using a custom pattern.
     *
     * @param lt     the time to format
     * @param pattern the pattern to use
     * @return the formatted string
     */
    public static String format(LocalTime lt, String pattern) {
        return format(lt, DateTimeFormatter.ofPattern(pattern));
    }

    private HighDate() {
    }

}
