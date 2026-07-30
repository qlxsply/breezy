package com.corwin.system.scheduler.domain.model;

import java.time.Duration;
import java.time.Instant;

/**
 * Schedule rule for fixed-interval execution (fixed-rate or fixed-delay).
 *
 * @author Corwin 2026/4/15
 */
public record FixedIntervalRule(
        Duration interval,
        Instant startAt,
        IntervalMode mode
) implements ScheduleRule {
}
