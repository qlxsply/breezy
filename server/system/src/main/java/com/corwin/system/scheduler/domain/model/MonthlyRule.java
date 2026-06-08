package com.corwin.system.scheduler.domain.model;

import java.time.LocalTime;
import java.time.ZoneId;

/**
 * 每月规则。
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
