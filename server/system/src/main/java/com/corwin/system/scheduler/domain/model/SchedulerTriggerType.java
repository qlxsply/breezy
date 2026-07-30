package com.corwin.system.scheduler.domain.model;

/**
 * Enum representing how a job execution was triggered.
 *
 * @author Corwin 2026/4/15
 */
public enum SchedulerTriggerType {
    SCHEDULED,
    MANUAL,
    RECOVERY
}
