package com.corwin.framework.cache;

import java.time.Duration;
import java.util.Optional;

/**
 * Result of a TTL query — existence, persistence flag, and optional remaining duration.
 *
 * @author Corwin 2026/4/19
 */
public record CacheTtlResult(boolean exists, boolean persistent, Optional<Duration> ttl) {

  public static CacheTtlResult notExists() {
    return new CacheTtlResult(false, false, Optional.empty());
  }

  public static CacheTtlResult persistentValue() {
    return new CacheTtlResult(true, true, Optional.empty());
  }

  public static CacheTtlResult expiring(Duration ttl) {
    return new CacheTtlResult(true, false, Optional.of(ttl));
  }
}
