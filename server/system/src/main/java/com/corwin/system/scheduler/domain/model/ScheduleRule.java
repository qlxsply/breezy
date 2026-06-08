package com.corwin.system.scheduler.domain.model;

/**
 * 调度规则抽象。
 *
 * @author Corwin 2026/4/15
 */
public sealed interface ScheduleRule
        permits OnceRule, FixedIntervalRule, DailyRule, WeeklyRule, MonthlyRule, YearlyRule {
}
