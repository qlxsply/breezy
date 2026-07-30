package com.corwin.system.config.interfaces.web.res;

/**
 * Response DTO for time offset preview.
 * Provides server and target timestamps along with the calculated offset
 * and mocked epoch millis for preview purposes.
 *
 * @author Corwin 2026/3/11
 */
public record ConfigTimeOffsetPreviewRes(
        long serverNowEpochMillis,
        long targetEpochMillis,
        Long calculatedOffsetSeconds,
        long offsetSeconds,
        long mockedEpochMillis) {
}
