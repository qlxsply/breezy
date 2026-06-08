package com.corwin.system.config.application.view;

/**
 * @author Corwin 2026/3/11
 */
public record ConfigTimeOffsetPreviewView(
        long serverNowEpochMillis,
        long targetEpochMillis,
        Long calculatedOffsetSeconds,
        long offsetSeconds,
        long mockedEpochMillis) {
}
