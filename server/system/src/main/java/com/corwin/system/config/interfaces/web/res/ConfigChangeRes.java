package com.corwin.system.config.interfaces.web.res;

/**
 * @author Corwin 2026/7/31
 */
public record ConfigChangeRes(
        String key,
        long persistedRevision,
        boolean pendingRestart
) {
}
