package com.corwin.system.diagnostic.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Domain model encapsulating the configuration parameters for a diagnostic session.
 * Performs validation and clamping of all numeric fields in the compact constructor.
 *
 * @author Corwin 2026/4/16
 */
public record DiagnosticConfig(
        long intervalMs,
        int historyCapacity,
        int eventCapacity,
        Set<DiagnosticItem> items,
        boolean deepMode,
        long slowRequestThresholdMs,
        long slowSqlThresholdMs,
        long ttlSeconds
) {

    public static final long DEFAULT_INTERVAL_MS = 5_000L;
    public static final int DEFAULT_HISTORY_CAPACITY = 180;
    public static final int DEFAULT_EVENT_CAPACITY = 300;
    public static final long DEFAULT_SLOW_REQUEST_THRESHOLD_MS = 1_000L;
    public static final long DEFAULT_SLOW_SQL_THRESHOLD_MS = 500L;
    public static final long DEFAULT_TTL_SECONDS = 1_800L;
    public static final long MIN_INTERVAL_MS = 1_000L;
    public static final long MAX_INTERVAL_MS = 60_000L;
    public static final int MIN_HISTORY_CAPACITY = 10;
    public static final int MAX_HISTORY_CAPACITY = 2_000;
    public static final int MIN_EVENT_CAPACITY = 10;
    public static final int MAX_EVENT_CAPACITY = 5_000;
    public static final long MIN_THRESHOLD_MS = 1L;
    public static final long MAX_TTL_SECONDS = 86_400L;

    /**
     * Compact constructor that validates and clamps all numeric fields within their allowed ranges,
     * and normalises the items set.
     */
    public DiagnosticConfig {
        intervalMs = clamp(intervalMs, MIN_INTERVAL_MS, MAX_INTERVAL_MS, DEFAULT_INTERVAL_MS);
        historyCapacity = clamp(historyCapacity, MIN_HISTORY_CAPACITY, MAX_HISTORY_CAPACITY, DEFAULT_HISTORY_CAPACITY);
        eventCapacity = clamp(eventCapacity, MIN_EVENT_CAPACITY, MAX_EVENT_CAPACITY, DEFAULT_EVENT_CAPACITY);
        items = normalizeItems(items);
        slowRequestThresholdMs = clamp(slowRequestThresholdMs, MIN_THRESHOLD_MS, Long.MAX_VALUE,
                DEFAULT_SLOW_REQUEST_THRESHOLD_MS);
        slowSqlThresholdMs = clamp(slowSqlThresholdMs, MIN_THRESHOLD_MS, Long.MAX_VALUE, DEFAULT_SLOW_SQL_THRESHOLD_MS);
        ttlSeconds = clamp(ttlSeconds, 60L, MAX_TTL_SECONDS, DEFAULT_TTL_SECONDS);
    }

    /**
     * Returns a default configuration with all diagnostic items enabled and standard thresholds.
     *
     * @return the default diagnostic configuration
     */
    public static DiagnosticConfig defaultConfig() {
        return new DiagnosticConfig(DEFAULT_INTERVAL_MS, DEFAULT_HISTORY_CAPACITY, DEFAULT_EVENT_CAPACITY,
                EnumSet.allOf(DiagnosticItem.class), false, DEFAULT_SLOW_REQUEST_THRESHOLD_MS,
                DEFAULT_SLOW_SQL_THRESHOLD_MS, DEFAULT_TTL_SECONDS);
    }

    /**
     * Checks whether the given diagnostic item is enabled in this configuration.
     *
     * @param item the diagnostic item to check
     * @return true if the item is included and non-null
     */
    public boolean includes(DiagnosticItem item) {
        return item != null && items.contains(item);
    }

    private static Set<DiagnosticItem> normalizeItems(Set<DiagnosticItem> items) {
        if (items == null || items.isEmpty()) {
            return Set.copyOf(EnumSet.allOf(DiagnosticItem.class));
        }
        EnumSet<DiagnosticItem> normalized = EnumSet.copyOf(items);
        return Set.copyOf(normalized);
    }

    private static long clamp(long value, long min, long max, long fallback) {
        if (value <= 0L) {
            return fallback;
        }
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static int clamp(int value, int min, int max, int fallback) {
        if (value <= 0) {
            return fallback;
        }
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
