package com.corwin.system.methodstat.domain.model;

import com.corwin.framework.event.model.AsyncEvent;
import java.util.Objects;

/**
 * Domain event representing a single method invocation, carrying timing, success status, and
 * exception details. Implements {@code AsyncEvent} for asynchronous processing.
 *
 * @author Corwin 2026/3/25
 */
public record MethodStatInvocationEvent(
    MethodStatKey key,
    long startedAtMillis,
    long finishedAtMillis,
    long durationMillis,
    boolean success,
    String exceptionClassName)
    implements AsyncEvent {

  public MethodStatInvocationEvent {
    Objects.requireNonNull(key, "key required");
    if (durationMillis < 0) {
      durationMillis = 0L;
    }
  }
}
