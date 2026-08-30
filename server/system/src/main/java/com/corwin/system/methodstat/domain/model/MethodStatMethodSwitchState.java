package com.corwin.system.methodstat.domain.model;

import java.util.Objects;

/**
 * Domain model representing the enabled/disabled switch state for a specific method.
 *
 * @author Corwin 2026/3/25
 */
public record MethodStatMethodSwitchState(MethodStatKey key, boolean enabled) {

  public MethodStatMethodSwitchState {
    Objects.requireNonNull(key, "key required");
  }
}
