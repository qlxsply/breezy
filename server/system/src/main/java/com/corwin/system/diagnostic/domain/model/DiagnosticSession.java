package com.corwin.system.diagnostic.domain.model;

import java.time.Instant;

/**
 * Domain model representing a diagnostic session with its status, configuration, and time bounds.
 *
 * @author Corwin 2026/4/16
 */
public record DiagnosticSession(
    DiagnosticStatus status, DiagnosticConfig config, Instant startedAt, Instant expireAt) {

  /**
   * Compact constructor that defaults the status to INACTIVE and config to defaultConfig when null.
   */
  public DiagnosticSession {
    status = status == null ? DiagnosticStatus.INACTIVE : status;
    config = config == null ? DiagnosticConfig.defaultConfig() : config;
  }

  /**
   * Creates a new active diagnostic session with the given config and start time.
   *
   * @param config the diagnostic configuration
   * @param now the session start instant; defaults to Instant.now() if null
   * @return a new ACTIVE session
   */
  public static DiagnosticSession active(DiagnosticConfig config, Instant now) {
    Instant startedAt = now == null ? Instant.now() : now;
    return new DiagnosticSession(
        DiagnosticStatus.ACTIVE, config, startedAt, startedAt.plusSeconds(config.ttlSeconds()));
  }

  /**
   * Checks whether the session is currently active (status is ACTIVE and not expired).
   *
   * @param now the reference instant
   * @return true if the session is active and not expired
   */
  public boolean isActive(Instant now) {
    return status == DiagnosticStatus.ACTIVE && !isExpired(now);
  }

  /**
   * Checks whether the session has expired based on its expireAt time.
   *
   * @param now the reference instant
   * @return true if the session has expired
   */
  public boolean isExpired(Instant now) {
    if (status != DiagnosticStatus.ACTIVE || expireAt == null) {
      return false;
    }
    Instant reference = now == null ? Instant.now() : now;
    return !reference.isBefore(expireAt);
  }

  /**
   * Calculates the remaining time-to-live in seconds from the given reference instant.
   *
   * @param now the reference instant
   * @return remaining seconds, or 0 if expired or expireAt is null
   */
  public long remainingTtlSeconds(Instant now) {
    if (expireAt == null) {
      return 0L;
    }
    Instant reference = now == null ? Instant.now() : now;
    if (!reference.isBefore(expireAt)) {
      return 0L;
    }
    return expireAt.getEpochSecond() - reference.getEpochSecond();
  }

  /**
   * Returns a new session with the given configuration, preserving the original status and
   * recalculating the expire time from the provided instant.
   *
   * @param newConfig the new diagnostic configuration
   * @param now the reference instant for the new expire time
   * @return a new session with the updated configuration
   */
  public DiagnosticSession withConfig(DiagnosticConfig newConfig, Instant now) {
    Instant reference = now == null ? Instant.now() : now;
    return new DiagnosticSession(
        status,
        newConfig,
        startedAt == null ? reference : startedAt,
        reference.plusSeconds(newConfig.ttlSeconds()));
  }

  /**
   * Returns a new session with status set to INACTIVE, preserving the original config and
   * timestamps.
   *
   * @return a stopped session
   */
  public DiagnosticSession stop() {
    return new DiagnosticSession(DiagnosticStatus.INACTIVE, config, startedAt, expireAt);
  }
}
