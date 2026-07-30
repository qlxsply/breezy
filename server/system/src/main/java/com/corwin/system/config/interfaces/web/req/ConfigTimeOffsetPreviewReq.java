package com.corwin.system.config.interfaces.web.req;

/**
 * Request DTO for time offset preview.
 * Accepts either an offset in seconds or a target epoch millis value
 * to calculate and preview the time shift.
 *
 * @author Corwin 2026/3/11
 */
public record ConfigTimeOffsetPreviewReq(
        Long offsetSeconds,
        Long targetEpochMillis
) {
}
