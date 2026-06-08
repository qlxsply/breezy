package com.corwin.system.scheduler.domain.model;

import java.time.Instant;

/**
 * 单次执行规则。
 *
 * @author Corwin 2026/4/15
 */
public record OnceRule(Instant executeAt) implements ScheduleRule {
}
