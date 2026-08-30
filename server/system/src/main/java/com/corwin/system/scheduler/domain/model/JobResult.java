package com.corwin.system.scheduler.domain.model;

import com.corwin.framework.error.BaseError;

/**
 * Record representing the result of a job execution with success status, code and message.
 *
 * @author Corwin 2026/4/15
 */
public record JobResult(boolean success, String code, String message) {

  /** Creates a success result with the given message. */
  public static JobResult success(String message) {
    return new JobResult(true, BaseError.SUCCESS.getCode(), message);
  }

  /** Creates a failure result with the given error code and message. */
  public static JobResult failure(String code, String message) {
    return new JobResult(false, code, message);
  }
}
