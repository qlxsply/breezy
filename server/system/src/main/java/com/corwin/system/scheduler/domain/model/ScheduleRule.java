package com.corwin.system.scheduler.domain.model;

/**
 * Sealed interface for all schedule rule types.
 *
 * @author Corwin 2026/4/15
 */
public sealed interface ScheduleRule
        permits OnceRule, FixedIntervalRule, DailyRule, WeeklyRule, MonthlyRule, YearlyRule {
}
