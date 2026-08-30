package com.corwin.system.scheduler.domain.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;

/**
 * Schedule rule for weekly execution on specific days at a fixed time.
 *
 * @author Corwin 2026/4/15
 */
public record WeeklyRule(Set<DayOfWeek> daysOfWeek, LocalTime time, ZoneId zoneId)
    implements ScheduleRule {}
