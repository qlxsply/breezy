package com.corwin.system.config.interfaces.web.res;

/**
 * @author Corwin 2026/3/11
 */
public record ConfigTimeOffsetPreviewRes(
        long serverNowEpochMillis,
        long targetEpochMillis,
        Long calculatedOffsetSeconds,
        long offsetSeconds,
        long mockedEpochMillis) {
}
