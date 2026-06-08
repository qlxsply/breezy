package com.corwin.system.scheduler.domain.model;

import java.time.Duration;
import java.time.Instant;

/**
 * 固定间隔规则。
 *
 * @author Corwin 2026/4/15
 */
public record FixedIntervalRule(
        Duration interval,
        Instant startAt,
        IntervalMode mode
) implements ScheduleRule {
}
