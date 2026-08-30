package com.corwin.reminder.infrastructure.schedule;

import com.corwin.reminder.application.port.NextOccurrenceCalculator;
import com.corwin.reminder.domain.model.RecurrenceRule;
import com.corwin.reminder.domain.model.RecurrenceRuleFrequency;
import com.corwin.reminder.domain.model.ScheduleEvent;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @author Corwin 2026/1/12
 */
@Component
public class SimpleNextOccurrenceCalculator implements NextOccurrenceCalculator {

  @Override
  public Instant nextStart(ScheduleEvent event, Instant fromExclusive) {
    if (event == null || event.getRule() == null) {
      return null;
    }
    if (!event.isActive()) {
      return null;
    }

    RecurrenceRule rule = event.getRule();
    Instant until = rule.getUntil();

    if (until != null && !until.isAfter(fromExclusive)) {
      return null;
    }

    if (rule.getFrequency() == RecurrenceRuleFrequency.ONCE) {
      Instant t = event.getStartTime();
      if (t != null && t.isAfter(fromExclusive) && (until == null || !t.isAfter(until))) {
        return t;
      }
      return null;
    }

    ZoneId eventZone = ZoneId.of(event.getEventTimeZoneId());

    ZonedDateTime start = ZonedDateTime.ofInstant(event.getStartTime(), eventZone);
    ZonedDateTime from =
        ZonedDateTime.ofInstant(fromExclusive.plusNanos(1), eventZone); // strict > exclusive

    ZonedDateTime next;
    switch (rule.getFrequency()) {
      case DAILY -> next = nextDaily(start, from, rule.getInterval());
      case WEEKLY -> next = nextWeekly(start, from, rule.getInterval(), rule.getDaysOfWeek());
      case MONTHLY -> next = nextMonthly(start, from, rule.getInterval(), rule.getDayOfMonth());
      case YEARLY ->
          next =
              nextYearly(
                  start, from, rule.getInterval(), rule.getMonthOfYear(), rule.getDayOfMonth());
      default -> next = null;
    }

    if (next == null) {
      return null;
    }

    Instant nextInstant = next.toInstant();
    if (until != null && nextInstant.isAfter(until)) {
      return null;
    }

    return nextInstant;
  }

  private ZonedDateTime nextDaily(ZonedDateTime start, ZonedDateTime from, int intervalDays) {
    // 以 start 的“时分秒”对齐，每 intervalDays 天一次
    ZonedDateTime candidate =
        from.withHour(start.getHour())
            .withMinute(start.getMinute())
            .withSecond(start.getSecond())
            .withNano(start.getNano());
    if (!candidate.isAfter(from)) {
      candidate = candidate.plusDays(1);
    }

    // 若 candidate 在 start 之前，需要拉到 start 之后
    if (candidate.isBefore(start)) {
      candidate = start;
    }

    // 对齐到 interval
    long daysBetween =
        Duration.between(
                start.toLocalDate().atStartOfDay(start.getZone()),
                candidate.toLocalDate().atStartOfDay(candidate.getZone()))
            .toDays();
    long mod = daysBetween % intervalDays;
    if (mod != 0) {
      candidate = candidate.plusDays(intervalDays - mod);
    }
    return candidate;
  }

  private ZonedDateTime nextWeekly(
      ZonedDateTime start, ZonedDateTime from, int intervalWeeks, Set<DayOfWeek> days) {
    if (days == null || days.isEmpty()) {
      return null;
    }

    // 从 from 的下一分钟（保持 start 的时分秒）开始找最近满足 dayOfWeek 的日期
    ZonedDateTime base =
        from.withHour(start.getHour())
            .withMinute(start.getMinute())
            .withSecond(start.getSecond())
            .withNano(start.getNano());
    if (!base.isAfter(from)) {
      base = base.plusMinutes(1);
    }

    // 若 base 在 start 之前，base = start
    if (base.isBefore(start)) {
      base = start;
    }

    // 逐天向前找，最多找 intervalWeeks*7 + 7 天即可命中一轮
    // 并确保周间隔对齐：以 start 所在周为第0周
    for (int i = 0; i < intervalWeeks * 7 + 7; i++) {
      ZonedDateTime c = base.plusDays(i);
      if (!days.contains(c.getDayOfWeek())) {
        continue;
      }

      long weeksBetween = weeksBetweenStartWeek(start, c);
      if (weeksBetween % intervalWeeks != 0) {
        continue;
      }

      if (c.isAfter(from)) {
        return c;
      }
    }
    return null;
  }

  private long weeksBetweenStartWeek(ZonedDateTime start, ZonedDateTime candidate) {
    // 以“周一”为一周起点进行对齐（可改为周日，看你的业务）
    LocalDate s = start.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    LocalDate c = candidate.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    return Duration.between(s.atStartOfDay(start.getZone()), c.atStartOfDay(candidate.getZone()))
            .toDays()
        / 7;
  }

  private ZonedDateTime nextMonthly(
      ZonedDateTime start, ZonedDateTime from, int intervalMonths, Integer dayOfMonth) {
    if (dayOfMonth == null) {
      return null;
    }

    ZonedDateTime base =
        from.withHour(start.getHour())
            .withMinute(start.getMinute())
            .withSecond(start.getSecond())
            .withNano(start.getNano());
    if (!base.isAfter(from)) {
      base = base.plusMinutes(1);
    }
    if (base.isBefore(start)) {
      base = start;
    }

    // 从 base 所在月开始试
    YearMonth ym = YearMonth.from(base);
    for (int i = 0; i < intervalMonths * 24 + 24; i++) { // 最多试一段合理范围
      YearMonth candYm = ym.plusMonths(i);
      if (!monthAligned(start, candYm, intervalMonths)) {
        continue;
      }

      int dom = Math.min(dayOfMonth, candYm.lengthOfMonth()); // 31号在小月自动落到月底
      ZonedDateTime c = candYm.atDay(dom).atTime(start.toLocalTime()).atZone(start.getZone());
      if (c.isBefore(start)) {
        continue;
      }
      if (c.isAfter(from)) {
        return c;
      }
    }
    return null;
  }

  private boolean monthAligned(ZonedDateTime start, YearMonth candYm, int intervalMonths) {
    YearMonth startYm = YearMonth.from(start);
    long monthsBetween =
        (candYm.getYear() - startYm.getYear()) * 12L
            + (candYm.getMonthValue() - startYm.getMonthValue());
    return monthsBetween >= 0 && monthsBetween % intervalMonths == 0;
  }

  private ZonedDateTime nextYearly(
      ZonedDateTime start,
      ZonedDateTime from,
      int intervalYears,
      Integer monthOfYear,
      Integer dayOfMonth) {
    if (monthOfYear == null || dayOfMonth == null) {
      return null;
    }

    ZonedDateTime base =
        from.withHour(start.getHour())
            .withMinute(start.getMinute())
            .withSecond(start.getSecond())
            .withNano(start.getNano());
    if (!base.isAfter(from)) {
      base = base.plusMinutes(1);
    }
    if (base.isBefore(start)) {
      base = start;
    }

    int startYear = start.getYear();
    int baseYear = base.getYear();

    for (int y = baseYear; y <= baseYear + intervalYears * 30; y++) {
      int yearsBetween = y - startYear;
      if (yearsBetween < 0 || yearsBetween % intervalYears != 0) {
        continue;
      }

      YearMonth ym = YearMonth.of(y, monthOfYear);
      int dom = Math.min(dayOfMonth, ym.lengthOfMonth());
      ZonedDateTime c = ym.atDay(dom).atTime(start.toLocalTime()).atZone(start.getZone());
      if (c.isBefore(start)) {
        continue;
      }
      if (c.isAfter(from)) {
        return c;
      }
    }
    return null;
  }
}
