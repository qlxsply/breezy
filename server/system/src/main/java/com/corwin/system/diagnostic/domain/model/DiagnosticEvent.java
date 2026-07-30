package com.corwin.system.diagnostic.domain.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Domain model representing a single diagnostic event (e.g. slow request, SQL error, JFR event).
 *
 * @author Corwin 2026/4/16
 */
public record DiagnosticEvent(
        String id,
        DiagnosticEventType type,
        Instant happenedAt,
        String title,
        String message,
        Map<String, Object> details
) {

    /**
     * Compact constructor that normalises an empty or null details map to an immutable empty map.
     */
    public DiagnosticEvent {
        details = (details == null || details.isEmpty()) ? Map.of() : Map.copyOf(new LinkedHashMap<>(details));
    }
}
