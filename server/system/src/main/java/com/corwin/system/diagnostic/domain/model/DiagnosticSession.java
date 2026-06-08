package com.corwin.system.diagnostic.domain.model;

import java.time.Instant;

/**
 * @author Corwin 2026/4/16
 */
public record DiagnosticSession(
        DiagnosticStatus status,
        DiagnosticConfig config,
        Instant startedAt,
        Instant expireAt
) {

    public DiagnosticSession {
        status = status == null ? DiagnosticStatus.INACTIVE : status;
        config = config == null ? DiagnosticConfig.defaultConfig() : config;
    }

    public static DiagnosticSession active(DiagnosticConfig config, Instant now) {
        Instant startedAt = now == null ? Instant.now() : now;
        return new DiagnosticSession(DiagnosticStatus.ACTIVE, config, startedAt,
                startedAt.plusSeconds(config.ttlSeconds()));
    }

    public boolean isActive(Instant now) {
        return status == DiagnosticStatus.ACTIVE && !isExpired(now);
    }

    public boolean isExpired(Instant now) {
        if (status != DiagnosticStatus.ACTIVE || expireAt == null) {
            return false;
        }
        Instant reference = now == null ? Instant.now() : now;
        return !reference.isBefore(expireAt);
    }

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

    public DiagnosticSession withConfig(DiagnosticConfig newConfig, Instant now) {
        Instant reference = now == null ? Instant.now() : now;
        return new DiagnosticSession(status, newConfig, startedAt == null ? reference : startedAt,
                reference.plusSeconds(newConfig.ttlSeconds()));
    }

    public DiagnosticSession stop() {
        return new DiagnosticSession(DiagnosticStatus.INACTIVE, config, startedAt, expireAt);
    }
}
