package com.corwin.system.scheduler.domain.model;

/**
 * Enum representing the result of a job execution.
 *
 * @author Corwin 2026/4/15
 */
public enum SchedulerExecutionResult {
  SUCCESS,
  FAILED,
  CANCELLED,
  SKIPPED
}
