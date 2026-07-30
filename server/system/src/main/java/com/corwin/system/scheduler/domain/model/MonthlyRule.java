package com.corwin.system.scheduler.domain.model;

import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Schedule rule for monthly execution on a specific day or the last day of the month.
 *
 * @author Corwin 2026/4/15
 */
public record MonthlyRule(
        MonthlyDayMode dayMode,
        Integer dayOfMonth,
        LocalTime time,
        ZoneId zoneId
) implements ScheduleRule {
}
