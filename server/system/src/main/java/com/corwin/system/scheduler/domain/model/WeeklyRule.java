package com.corwin.system.scheduler.domain.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;

/**
 * 每周规则。
 *
 * @author Corwin 2026/4/15
 */
public record WeeklyRule(
        Set<DayOfWeek> daysOfWeek,
        LocalTime time,
        ZoneId zoneId
) implements ScheduleRule {
}
