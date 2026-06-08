package com.corwin.system.scheduler.domain.model;

import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;

/**
 * 每年规则。
 *
 * @author Corwin 2026/4/15
 */
public record YearlyRule(
        Month month,
        int dayOfMonth,
        LocalTime time,
        ZoneId zoneId
) implements ScheduleRule {
}
