package com.corwin.system.scheduler.domain.model;

import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Schedule rule for daily execution at a fixed time.
 *
 * @author Corwin 2026/4/15
 */
public record DailyRule(
        LocalTime time,
        ZoneId zoneId
) implements ScheduleRule {
}
