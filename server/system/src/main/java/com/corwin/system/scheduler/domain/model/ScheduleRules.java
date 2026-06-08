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
 * 调度规则工厂。
 *
 * @author Corwin 2026/4/15
 */
public final class ScheduleRules {

    private ScheduleRules() {
    }

    public static OnceRule once(Instant executeAt) {
        return new OnceRule(Objects.requireNonNull(executeAt, "executeAt required"));
    }

    public static FixedIntervalRule fixedDelay(Duration delay) {
        return new FixedIntervalRule(Objects.requireNonNull(delay, "delay required"), HighDate.mockInstant(),
                IntervalMode.FIXED_DELAY);
    }

    public static FixedIntervalRule fixedDelay(Duration delay, Instant startAt) {
        return new FixedIntervalRule(Objects.requireNonNull(delay, "delay required"),
                Objects.requireNonNull(startAt, "startAt required"), IntervalMode.FIXED_DELAY);
    }

    public static FixedIntervalRule fixedRate(Duration rate) {
        return new FixedIntervalRule(Objects.requireNonNull(rate, "rate required"), HighDate.mockInstant(),
                IntervalMode.FIXED_RATE);
    }

    public static FixedIntervalRule fixedRate(Duration rate, Instant startAt) {
        return new FixedIntervalRule(Objects.requireNonNull(rate, "rate required"),
                Objects.requireNonNull(startAt, "startAt required"), IntervalMode.FIXED_RATE);
    }

    public static DailyRule daily(int hour, int minute) {
        return new DailyRule(LocalTime.of(hour, minute), HighDate.zone());
    }

    public static DailyRule daily(int hour, int minute, ZoneId zoneId) {
        return new DailyRule(LocalTime.of(hour, minute), Objects.requireNonNull(zoneId, "zoneId required"));
    }

    public static WeeklyRule weekly(DayOfWeek dayOfWeek, int hour, int minute) {
        return new WeeklyRule(Set.of(Objects.requireNonNull(dayOfWeek, "dayOfWeek required")),
                LocalTime.of(hour, minute), HighDate.zone());
    }

    public static WeeklyRule weekly(Set<DayOfWeek> daysOfWeek, int hour, int minute) {
        return new WeeklyRule(Set.copyOf(daysOfWeek), LocalTime.of(hour, minute), HighDate.zone());
    }

    public static MonthlyRule monthly(int dayOfMonth, int hour, int minute) {
        return new MonthlyRule(MonthlyDayMode.EXACT_DAY, dayOfMonth, LocalTime.of(hour, minute), HighDate.zone());
    }

    public static MonthlyRule monthlyLastDay(int hour, int minute) {
        return new MonthlyRule(MonthlyDayMode.LAST_DAY, null, LocalTime.of(hour, minute), HighDate.zone());
    }

    public static YearlyRule yearly(Month month, int dayOfMonth, int hour, int minute) {
        return new YearlyRule(Objects.requireNonNull(month, "month required"), dayOfMonth, LocalTime.of(hour, minute),
                HighDate.zone());
    }
}
