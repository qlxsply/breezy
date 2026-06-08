package com.corwin.system.diagnostic.domain.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
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

    public DiagnosticEvent {
        details = (details == null || details.isEmpty()) ? Map.of() : Map.copyOf(new LinkedHashMap<>(details));
    }
}
