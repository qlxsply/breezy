package com.corwin.framework.config.runtime;

import com.corwin.framework.config.definition.ConfigKey;
import java.time.Instant;
import java.util.Objects;

/**
 * @author Corwin 2026/7/30
 */
public record ConfigSnapshot<T>(
    ConfigKey key,
    T value,
    long revision,
    int schemaVersion,
    ConfigValueSource source,
    Instant loadedAt,
    String loadWarning) {

  public ConfigSnapshot {
    Objects.requireNonNull(key, "key required");
    Objects.requireNonNull(value, "value required");
    if (revision < 0) {
      throw new IllegalArgumentException("revision must not be negative");
    }
    if (schemaVersion < 1) {
      throw new IllegalArgumentException("schemaVersion must be greater than zero");
    }
    Objects.requireNonNull(source, "source required");
    Objects.requireNonNull(loadedAt, "loadedAt required");
    loadWarning = loadWarning == null ? "" : loadWarning;
  }
}
