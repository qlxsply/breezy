package com.corwin.system.scheduler.infrastructure.scheduling;

import com.corwin.framework.util.HighDate;
import com.corwin.system.scheduler.domain.model.DailyRule;
import com.corwin.system.scheduler.domain.model.FixedIntervalRule;
import com.corwin.system.scheduler.domain.model.IntervalMode;
import com.corwin.system.scheduler.domain.model.MonthlyDayMode;
import com.corwin.system.scheduler.domain.model.MonthlyRule;
import com.corwin.system.scheduler.domain.model.OnceRule;
import com.corwin.system.scheduler.domain.model.ScheduleRule;
import com.corwin.system.scheduler.domain.model.WeeklyRule;
import com.corwin.system.scheduler.domain.model.YearlyRule;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Comparator;

/**
 * Spring {@link Trigger} implementation that computes next execution times from a {@link ScheduleRule}.
 *
 * @author Corwin 2026/4/15
 */
public class ScheduleRuleTrigger implements Trigger {

    private final ScheduleRule scheduleRule;

    public ScheduleRuleTrigger(ScheduleRule scheduleRule) {
        this.scheduleRule = scheduleRule;
    }

    /** Computes the next execution time based on the schedule rule and trigger context. */
    @Override
    @Nullable
    public Instant nextExecution(TriggerContext triggerContext) {
        Instant now = HighDate.realInstant();
        Instant lastScheduledExecution = triggerContext.lastScheduledExecution();
        Instant lastCompletion = triggerContext.lastCompletion();
        return nextExecution(scheduleRule, lastScheduledExecution, lastCompletion, now);
    }

    /** Calculates the first scheduled execution time for a rule. */
    @Nullable
    public static Instant firstExecution(ScheduleRule rule) {
        return nextExecution(rule, null, null, HighDate.realInstant());
    }

    /** Calculates the next execution time after a given scheduled/completion time. */
    @Nullable
    public static Instant nextExecutionAfter(ScheduleRule rule, @Nullable Instant lastScheduledExecution,
            @Nullable Instant lastCompletion) {
        return nextExecution(rule, lastScheduledExecution, lastCompletion, HighDate.realInstant());
    }

    @Nullable
    private static Instant nextExecution(ScheduleRule rule, @Nullable Instant lastScheduledExecution,
            @Nullable Instant lastCompletion, Instant now) {
        return switch (rule) {
            case OnceRule onceRule -> nextOnce(onceRule, lastScheduledExecution, now);
            case FixedIntervalRule fixedIntervalRule -> nextFixedInterval(fixedIntervalRule, lastScheduledExecution,
                    lastCompletion, now);
            case DailyRule dailyRule -> nextDaily(dailyRule, now);
            case WeeklyRule weeklyRule -> nextWeekly(weeklyRule, now);
            case MonthlyRule monthlyRule -> nextMonthly(monthlyRule, now);
            case YearlyRule yearlyRule -> nextYearly(yearlyRule, now);
        };
    }

    private static Instant nextOnce(OnceRule rule, @Nullable Instant lastScheduledExecution, Instant now) {
        if (lastScheduledExecution != null) {
            return null;
        }
        return rule.executeAt().isAfter(now) ? rule.executeAt() : rule.executeAt();
    }

    private static Instant nextFixedInterval(FixedIntervalRule rule, @Nullable Instant lastScheduledExecution,
            @Nullable Instant lastCompletion, Instant now) {
        Instant base = rule.startAt() == null ? now : rule.startAt();
        if (lastScheduledExecution == null && lastCompletion == null) {
            return base.isAfter(now) ? base : now.plusMillis(1);
        }
        Instant nextBase = rule.mode() == IntervalMode.FIXED_RATE
                ? (lastScheduledExecution == null ? base : lastScheduledExecution)
                : (lastCompletion == null ? now : lastCompletion);
        return nextBase.plus(rule.interval());
    }

    private static Instant nextDaily(DailyRule rule, Instant now) {
        ZoneId zoneId = rule.zoneId();
        LocalDateTime nowAtZone = HighDate.ofDateTime(now, zoneId);
        LocalDate date = nowAtZone.toLocalDate();
        LocalDateTime target = LocalDateTime.of(date, rule.time());
        if (!target.atZone(zoneId).toInstant().isAfter(now)) {
            target = LocalDateTime.of(date.plusDays(1), rule.time());
        }
        return target.atZone(zoneId).toInstant();
    }

    private static Instant nextWeekly(WeeklyRule rule, Instant now) {
        ZoneId zoneId = rule.zoneId();
        LocalDateTime nowAtZone = HighDate.ofDateTime(now, zoneId);
        LocalDate today = nowAtZone.toLocalDate();
        return rule.daysOfWeek().stream()
                .map(dayOfWeek -> {
                    int offset = dayOfWeek.getValue() - today.getDayOfWeek().getValue();
                    if (offset < 0) {
                        offset += 7;
                    }
                    LocalDate candidateDate = today.plusDays(offset);
                    LocalDateTime candidate = LocalDateTime.of(candidateDate, rule.time());
                    if (!candidate.atZone(zoneId).toInstant().isAfter(now)) {
                        candidate = candidate.plusWeeks(1);
                    }
                    return candidate.atZone(zoneId).toInstant();
                })
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    private static Instant nextMonthly(MonthlyRule rule, Instant now) {
        ZoneId zoneId = rule.zoneId();
        LocalDateTime nowAtZone = HighDate.ofDateTime(now, zoneId);
        YearMonth current = YearMonth.from(nowAtZone);
        Instant currentCandidate = toMonthlyInstant(rule, current, zoneId);
        if (currentCandidate != null && currentCandidate.isAfter(now)) {
            return currentCandidate;
        }
        return toMonthlyInstant(rule, current.plusMonths(1), zoneId);
    }

    private static Instant nextYearly(YearlyRule rule, Instant now) {
        ZoneId zoneId = rule.zoneId();
        LocalDateTime nowAtZone = HighDate.ofDateTime(now, zoneId);
        LocalDateTime current = LocalDateTime.of(nowAtZone.getYear(), rule.month().getValue(), rule.dayOfMonth(),
                rule.time().getHour(), rule.time().getMinute(), rule.time().getSecond(), rule.time().getNano());
        Instant currentInstant = current.atZone(zoneId).toInstant();
        if (currentInstant.isAfter(now)) {
            return currentInstant;
        }
        return current.plusYears(1).atZone(zoneId).toInstant();
    }

    private static Instant toMonthlyInstant(MonthlyRule rule, YearMonth yearMonth, ZoneId zoneId) {
        int day = rule.dayMode() == MonthlyDayMode.LAST_DAY
                ? yearMonth.lengthOfMonth()
                : Math.min(rule.dayOfMonth(), yearMonth.lengthOfMonth());
        LocalDateTime candidate = LocalDateTime.of(yearMonth.getYear(), yearMonth.getMonthValue(), day,
                rule.time().getHour(), rule.time().getMinute(), rule.time().getSecond(), rule.time().getNano());
        return candidate.atZone(zoneId).toInstant();
    }
}
