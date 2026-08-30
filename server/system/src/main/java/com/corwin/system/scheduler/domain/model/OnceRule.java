package com.corwin.system.scheduler.domain.model;

import java.time.Instant;

/**
 * Schedule rule for one-time execution at a specific instant.
 *
 * @author Corwin 2026/4/15
 */
public record OnceRule(Instant executeAt) implements ScheduleRule {}
