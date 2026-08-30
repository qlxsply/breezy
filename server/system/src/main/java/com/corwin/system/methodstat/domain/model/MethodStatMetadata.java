package com.corwin.system.methodstat.domain.model;

import java.util.Objects;

/**
 * Domain record holding the registered metadata for a monitored method, including its fully
 * qualified name, class details, and switch state.
 *
 * @author Corwin 2026/3/25
 */
public record MethodStatMetadata(
    MethodStatKey key,
    String packageName,
    String className,
    String methodName,
    String methodSignature,
    boolean methodSwitchEnabled) {

  public MethodStatMetadata {
    Objects.requireNonNull(key, "key required");
    Objects.requireNonNull(packageName, "packageName required");
    Objects.requireNonNull(className, "className required");
    Objects.requireNonNull(methodName, "methodName required");
    Objects.requireNonNull(methodSignature, "methodSignature required");
  }

  /**
   * Create a new metadata instance with the method switch state updated.
   *
   * @param nextMethodSwitchEnabled the new method switch state
   * @return a new metadata record with the updated switch state
   */
  public MethodStatMetadata withMethodSwitchEnabled(boolean nextMethodSwitchEnabled) {
    return new MethodStatMetadata(
        key, packageName, className, methodName, methodSignature, nextMethodSwitchEnabled);
  }
}
