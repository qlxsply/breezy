package com.corwin.system.config.application.view;

/**
 * @author Corwin 2026/7/30
 */
public record ConfigChangeView(
        String key,
        long persistedRevision,
        boolean pendingRestart
) {
}
