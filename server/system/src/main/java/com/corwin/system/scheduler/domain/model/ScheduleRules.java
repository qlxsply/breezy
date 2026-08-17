package com.corwin.system.scheduler.domain.model;

import com.corwin.framework.util.HighDate;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Set;

/**
 * Factory with static helper methods for creating various {@link ScheduleRule} instances.
 *
 * @author Corwin 2026/4/15
 */
public final class ScheduleRules {

    private ScheduleRules() {
    }

    /** Creates a one-time execution rule. */
    public static OnceRule once(Instant executeAt) {
        return new OnceRule(Objects.requireNonNull(executeAt, "executeAt required"));
    }

    /** Creates a fixed-delay rule starting immediately. */
    public static FixedIntervalRule fixedDelay(Duration delay) {
        return new FixedIntervalRule(Objects.requireNonNull(delay, "delay required"), HighDate.realInstant(),
                IntervalMode.FIXED_DELAY);
    }

    /** Creates a fixed-delay rule starting at the given time. */
    public static FixedIntervalRule fixedDelay(Duration delay, Instant startAt) {
        return new FixedIntervalRule(Objects.requireNonNull(delay, "delay required"),
                Objects.requireNonNull(startAt, "startAt required"), IntervalMode.FIXED_DELAY);
    }

    /** Creates a fixed-rate rule starting immediately. */
    public static FixedIntervalRule fixedRate(Duration rate) {
        return new FixedIntervalRule(Objects.requireNonNull(rate, "rate required"), HighDate.realInstant(),
                IntervalMode.FIXED_RATE);
    }

    /** Creates a fixed-rate rule starting at the given time. */
    public static FixedIntervalRule fixedRate(Duration rate, Instant startAt) {
        return new FixedIntervalRule(Objects.requireNonNull(rate, "rate required"),
                Objects.requireNonNull(startAt, "startAt required"), IntervalMode.FIXED_RATE);
    }

    /** Creates a daily rule at the given hour and minute in the default timezone. */
    public static DailyRule daily(int hour, int minute) {
        return new DailyRule(LocalTime.of(hour, minute), HighDate.zone());
    }

    /** Creates a daily rule at the given hour and minute in the specified timezone. */
    public static DailyRule daily(int hour, int minute, ZoneId zoneId) {
        return new DailyRule(LocalTime.of(hour, minute), Objects.requireNonNull(zoneId, "zoneId required"));
    }

    /** Creates a weekly rule on the given day at the specified time. */
    public static WeeklyRule weekly(DayOfWeek dayOfWeek, int hour, int minute) {
        return new WeeklyRule(Set.of(Objects.requireNonNull(dayOfWeek, "dayOfWeek required")),
                LocalTime.of(hour, minute), HighDate.zone());
    }

    /** Creates a weekly rule on the given set of days at the specified time. */
    public static WeeklyRule weekly(Set<DayOfWeek> daysOfWeek, int hour, int minute) {
        return new WeeklyRule(Set.copyOf(daysOfWeek), LocalTime.of(hour, minute), HighDate.zone());
    }

    /** Creates a monthly rule on the exact day of month at the specified time. */
    public static MonthlyRule monthly(int dayOfMonth, int hour, int minute) {
        return new MonthlyRule(MonthlyDayMode.EXACT_DAY, dayOfMonth, LocalTime.of(hour, minute), HighDate.zone());
    }

    /** Creates a monthly rule on the last day of each month at the specified time. */
    public static MonthlyRule monthlyLastDay(int hour, int minute) {
        return new MonthlyRule(MonthlyDayMode.LAST_DAY, null, LocalTime.of(hour, minute), HighDate.zone());
    }

    /** Creates a yearly rule on the given month and day at the specified time. */
    public static YearlyRule yearly(Month month, int dayOfMonth, int hour, int minute) {
        return new YearlyRule(Objects.requireNonNull(month, "month required"), dayOfMonth, LocalTime.of(hour, minute),
                HighDate.zone());
    }
}
