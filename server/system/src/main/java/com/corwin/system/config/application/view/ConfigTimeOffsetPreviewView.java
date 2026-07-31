package com.corwin.system.config.application.view;

/**
 * View object for time offset preview.
 * Provides server time, target time, calculated offset, and mocked epoch millis.
 *
 * @author Corwin 2026/3/11
 */
public record ConfigTimeOffsetPreviewView(
        long serverNowEpochMillis,
        long targetEpochMillis,
        Long calculatedOffsetSeconds,
        long offsetSeconds,
        long mockedEpochMillis
) {
}
