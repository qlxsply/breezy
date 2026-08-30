package com.corwin.system.scheduler.domain.model;

/**
 * Enum representing the runtime status of a scheduled job.
 *
 * @author Corwin 2026/4/15
 */
public enum SchedulerJobStatus {
  SCHEDULED,
  RUNNING,
  PAUSED,
  CANCELLED,
  INVALID,
  ERROR
}
