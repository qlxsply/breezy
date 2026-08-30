package com.corwin.framework.util;

import com.corwin.framework.config.builtin.FrameworkConfigSpecs;
import com.corwin.framework.config.runtime.Configs;
import com.corwin.framework.constant.DateTimePatterns;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Primary date/time utility based on {@code java.time}.
 *
 * @author Corwin 2026/2/1
 */
public final class HighDate {

  public static ZoneId zone() {
    return ZoneId.systemDefault();
  }

  public static Instant realInstant() {
    return Instant.now();
  }

  public static LocalDateTime realDateTime() {
    return ofDateTime(realInstant());
  }

  public static LocalDate realDate() {
    return realDateTime().toLocalDate();
  }

  public static LocalTime realTime() {
    return realDateTime().toLocalTime();
  }

  public static long realTimestampMillis() {
    return realInstant().toEpochMilli();
  }

  /**
   * Returns the current business instant according to the configured mock mode. Does not depend on
   * time zone.
   *
   * @return the mocked {@link Instant}
   */
  public static Instant mockInstant() {
    var config = Configs.get(FrameworkConfigSpecs.TIME_MOCK);
    return switch (config.mode()) {
      case DYNAMIC -> realInstant().plusSeconds(config.offsetSeconds());
      case FIXED -> Instant.ofEpochMilli(config.fixedEpochMillis());
    };
  }

  public static LocalDateTime mockDateTime() {
    return ofDateTime(mockInstant());
  }

  public static LocalDate mockDate() {
    return mockDateTime().toLocalDate();
  }

  public static LocalTime mockTime() {
    return mockDateTime().toLocalTime();
  }

  public static long mockTimestampMillis() {
    return mockInstant().toEpochMilli();
  }

  public static Instant ofInstant(long epochMillis) {
    return Instant.ofEpochMilli(epochMillis);
  }

  public static Instant ofInstantSeconds(long epochSeconds) {
    return Instant.ofEpochSecond(epochSeconds);
  }

  public static LocalDateTime ofDateTime(long epochMillis) {
    return ofDateTime(ofInstant(epochMillis));
  }

  public static LocalDateTime ofDateTime(Instant instant) {
    return ofDateTime(instant, zone());
  }

  public static LocalDateTime ofDateTime(Instant instant, ZoneId zoneId) {
    return LocalDateTime.ofInstant(instant, zoneId);
  }

  public static LocalDate ofDate(long epochMillis) {
    return ofDateTime(epochMillis).toLocalDate();
  }

  public static LocalDate ofDate(Instant instant) {
    return ofDateTime(instant).toLocalDate();
  }

  public static LocalDate ofDate(Instant instant, ZoneId zoneId) {
    return ofDateTime(instant, zoneId).toLocalDate();
  }

  public static LocalTime ofTime(long epochMillis) {
    return ofDateTime(epochMillis).toLocalTime();
  }

  public static LocalTime ofTime(Instant instant) {
    return ofDateTime(instant).toLocalTime();
  }

  public static LocalTime ofTime(Instant instant, ZoneId zoneId) {
    return ofDateTime(instant, zoneId).toLocalTime();
  }

  public static ZonedDateTime ofZonedDateTime(Instant instant) {
    return instant.atZone(zone());
  }

  public static ZonedDateTime ofZonedDateTime(Instant instant, ZoneId zoneId) {
    return instant.atZone(zoneId);
  }

  public static Instant toInstant(LocalDateTime dateTime) {
    if (dateTime == null) {
      return null;
    }
    return dateTime.atZone(zone()).toInstant();
  }

  public static Instant toInstant(LocalDateTime dateTime, ZoneId zoneId) {
    if (dateTime == null) {
      return null;
    }
    return dateTime.atZone(zoneId).toInstant();
  }

  public static Instant toInstant(LocalDate date) {
    if (date == null) {
      return null;
    }
    return date.atStartOfDay(zone()).toInstant();
  }

  public static Instant toInstant(LocalDate date, ZoneId zoneId) {
    if (date == null) {
      return null;
    }
    return date.atStartOfDay(zoneId).toInstant();
  }

  public static Instant toInstant(LocalDate date, LocalTime time) {
    return toInstant(date, time, zone());
  }

  public static Instant toInstant(LocalDate date, LocalTime time, ZoneId zoneId) {
    if (date == null || time == null) {
      return null;
    }
    return LocalDateTime.of(date, time).atZone(zoneId).toInstant();
  }

  public static long toEpochMillis(LocalDateTime ldt) {
    if (ldt == null) {
      return 0L;
    }
    return toInstant(ldt).toEpochMilli();
  }

  public static long toEpochSeconds(LocalDateTime ldt) {
    if (ldt == null) {
      return 0L;
    }
    return toInstant(ldt).getEpochSecond();
  }

  public static long toEpochMillis(Instant instant) {
    if (instant == null) {
      return 0L;
    }
    return instant.toEpochMilli();
  }

  public static long toEpochSeconds(Instant instant) {
    if (instant == null) {
      return 0L;
    }
    return instant.getEpochSecond();
  }

  public static LocalDateTime toStartOfDay(LocalDateTime dateTime) {
    if (dateTime == null) {
      return null;
    }
    return dateTime.toLocalDate().atStartOfDay();
  }

  public static LocalDateTime toStartOfDay(LocalDate date) {
    if (date == null) {
      return null;
    }
    return date.atStartOfDay();
  }

  public static LocalDateTime toStartOfNextDay(LocalDateTime dateTime) {
    if (dateTime == null) {
      return null;
    }
    return dateTime.toLocalDate().plusDays(1).atStartOfDay();
  }

  public static LocalDateTime toStartOfNextDay(LocalDate date) {
    if (date == null) {
      return null;
    }
    return date.plusDays(1).atStartOfDay();
  }

  public static LocalDateTime toEndOfDay(LocalDateTime dateTime) {
    if (dateTime == null) {
      return null;
    }
    return toStartOfNextDay(dateTime).minusNanos(1);
  }

  public static LocalDateTime toEndOfDay(LocalDate date) {
    if (date == null) {
      return null;
    }
    return toStartOfNextDay(date).minusNanos(1);
  }

  public static Instant toStartOfDay(Instant instant) {
    return toStartOfDay(instant, zone());
  }

  public static Instant toStartOfDay(Instant instant, ZoneId zoneId) {
    if (instant == null) {
      return null;
    }
    return ofDate(instant, zoneId).atStartOfDay(zoneId).toInstant();
  }

  public static Instant toStartOfNextDay(Instant instant) {
    return toStartOfNextDay(instant, zone());
  }

  public static Instant toStartOfNextDay(Instant instant, ZoneId zoneId) {
    if (instant == null) {
      return null;
    }
    return ofDate(instant, zoneId).plusDays(1).atStartOfDay(zoneId).toInstant();
  }

  public static Instant toEndOfDay(Instant instant) {
    return toEndOfDay(instant, zone());
  }

  public static Instant toEndOfDay(Instant instant, ZoneId zoneId) {
    Instant nextDayStart = toStartOfNextDay(instant, zoneId);
    if (nextDayStart == null) {
      return null;
    }
    return nextDayStart.minusNanos(1);
  }

  public static LocalDateTime toStartOfMonth(LocalDateTime dateTime) {
    if (dateTime == null) {
      return null;
    }
    return dateTime.toLocalDate().withDayOfMonth(1).atStartOfDay();
  }

  public static LocalDateTime toStartOfNextMonth(LocalDateTime dateTime) {
    if (dateTime == null) {
      return null;
    }
    return dateTime.toLocalDate().withDayOfMonth(1).plusMonths(1).atStartOfDay();
  }

  public static LocalDateTime toStartOfYear(LocalDateTime dateTime) {
    if (dateTime == null) {
      return null;
    }
    return dateTime.toLocalDate().withDayOfYear(1).atStartOfDay();
  }

  public static LocalDateTime toStartOfNextYear(LocalDateTime dateTime) {
    if (dateTime == null) {
      return null;
    }
    return dateTime.toLocalDate().withDayOfYear(1).plusYears(1).atStartOfDay();
  }

  public static Instant toStartOfMonth(Instant instant) {
    return toStartOfMonth(instant, zone());
  }

  public static Instant toStartOfMonth(Instant instant, ZoneId zoneId) {
    if (instant == null) {
      return null;
    }
    LocalDate date = ofDate(instant, zoneId).withDayOfMonth(1);
    return date.atStartOfDay(zoneId).toInstant();
  }

  public static Instant toStartOfNextMonth(Instant instant) {
    return toStartOfNextMonth(instant, zone());
  }

  public static Instant toStartOfNextMonth(Instant instant, ZoneId zoneId) {
    if (instant == null) {
      return null;
    }
    LocalDate date = ofDate(instant, zoneId).withDayOfMonth(1).plusMonths(1);
    return date.atStartOfDay(zoneId).toInstant();
  }

  public static Instant toStartOfYear(Instant instant) {
    return toStartOfYear(instant, zone());
  }

  public static Instant toStartOfYear(Instant instant, ZoneId zoneId) {
    if (instant == null) {
      return null;
    }
    LocalDate date = ofDate(instant, zoneId).withDayOfYear(1);
    return date.atStartOfDay(zoneId).toInstant();
  }

  public static Instant toStartOfNextYear(Instant instant) {
    return toStartOfNextYear(instant, zone());
  }

  public static Instant toStartOfNextYear(Instant instant, ZoneId zoneId) {
    if (instant == null) {
      return null;
    }
    LocalDate date = ofDate(instant, zoneId).withDayOfYear(1).plusYears(1);
    return date.atStartOfDay(zoneId).toInstant();
  }

  public static long betweenMillis(Instant startInclusive, Instant endExclusive) {
    Objects.requireNonNull(startInclusive, "startInclusive required");
    Objects.requireNonNull(endExclusive, "endExclusive required");
    return endExclusive.toEpochMilli() - startInclusive.toEpochMilli();
  }

  public static long betweenSeconds(Instant startInclusive, Instant endExclusive) {
    Objects.requireNonNull(startInclusive, "startInclusive required");
    Objects.requireNonNull(endExclusive, "endExclusive required");
    return endExclusive.getEpochSecond() - startInclusive.getEpochSecond();
  }

  public static Instant min(Instant left, Instant right) {
    if (left == null) {
      return right;
    }
    if (right == null) {
      return left;
    }
    return left.isBefore(right) ? left : right;
  }

  public static Instant max(Instant left, Instant right) {
    if (left == null) {
      return right;
    }
    if (right == null) {
      return left;
    }
    return left.isAfter(right) ? left : right;
  }

  public static LocalDateTime parse(String str) {
    return parse(str, DateTimePatterns.DATE_TIME_FMT);
  }

  public static LocalDate parseDate(String str) {
    return parseDate(str, DateTimePatterns.DATE_FMT);
  }

  public static LocalTime parseTime(String str) {
    return parseTime(str, DateTimePatterns.TIME_FMT);
  }

  public static LocalDateTime parse(String str, DateTimeFormatter formatter) {
    return LocalDateTime.parse(str, formatter);
  }

  public static LocalDateTime parse(String str, String pattern) {
    return parse(str, DateTimeFormatter.ofPattern(pattern));
  }

  public static LocalDate parseDate(String str, DateTimeFormatter formatter) {
    return LocalDate.parse(str, formatter);
  }

  public static LocalDate parseDate(String str, String pattern) {
    return parseDate(str, DateTimeFormatter.ofPattern(pattern));
  }

  public static LocalTime parseTime(String str, DateTimeFormatter formatter) {
    return LocalTime.parse(str, formatter);
  }

  public static LocalTime parseTime(String str, String pattern) {
    return parseTime(str, DateTimeFormatter.ofPattern(pattern));
  }

  public static String format(LocalDateTime ldt) {
    return format(ldt, DateTimePatterns.DATE_TIME_FMT);
  }

  public static String format(LocalDate ld) {
    return format(ld, DateTimePatterns.DATE_FMT);
  }

  public static String format(LocalTime lt) {
    return format(lt, DateTimePatterns.TIME_FMT);
  }

  public static String format(LocalDateTime ldt, DateTimeFormatter formatter) {
    return ldt.format(formatter);
  }

  public static String format(LocalDateTime ldt, String pattern) {
    return format(ldt, DateTimeFormatter.ofPattern(pattern));
  }

  public static String format(LocalDate ld, DateTimeFormatter formatter) {
    return ld.format(formatter);
  }

  public static String format(LocalDate ld, String pattern) {
    return format(ld, DateTimeFormatter.ofPattern(pattern));
  }

  public static String format(LocalTime lt, DateTimeFormatter formatter) {
    return lt.format(formatter);
  }

  public static String format(LocalTime lt, String pattern) {
    return format(lt, DateTimeFormatter.ofPattern(pattern));
  }

  private HighDate() {}
}
