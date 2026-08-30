package com.corwin.system.scheduler.domain.model;

import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;

/**
 * Schedule rule for yearly execution on a specific month and day.
 *
 * @author Corwin 2026/4/15
 */
public record YearlyRule(Month month, int dayOfMonth, LocalTime time, ZoneId zoneId)
    implements ScheduleRule {}
