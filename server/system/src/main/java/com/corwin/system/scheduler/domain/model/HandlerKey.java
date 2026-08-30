package com.corwin.system.scheduler.domain.model;

import java.util.Objects;

/**
 * Value record representing a stable identifier for a job handler.
 *
 * @author Corwin 2026/4/15
 */
public record HandlerKey(String value) {

  /**
   * Constructs a HandlerKey after trimming and validating the value.
   *
   * @param value the handler key string
   * @throws IllegalArgumentException if value is null or blank
   */
  public HandlerKey {
    value = Objects.requireNonNull(value, "value required").trim();
    if (value.isEmpty()) {
      throw new IllegalArgumentException("value required");
    }
  }
}
