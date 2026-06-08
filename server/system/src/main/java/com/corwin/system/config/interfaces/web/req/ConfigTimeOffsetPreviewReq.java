package com.corwin.system.config.interfaces.web.req;

/**
 * @author Corwin 2026/3/11
 */
public record ConfigTimeOffsetPreviewReq(
        Long offsetSeconds,
        Long targetEpochMillis
) {
}
