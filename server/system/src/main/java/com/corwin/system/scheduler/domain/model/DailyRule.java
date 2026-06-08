package com.corwin.system.scheduler.domain.model;

import java.time.LocalTime;
import java.time.ZoneId;

/**
 * 每日规则。
 *
 * @author Corwin 2026/4/15
 */
public record DailyRule(
        LocalTime time,
        ZoneId zoneId
) implements ScheduleRule {
}
