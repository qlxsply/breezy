package com.corwin.system.config.application.command;

/**
 * @author Corwin 2026/7/30
 */
public record ResetConfigCommand(
        String configKey,
        long expectedRevision,
        String reason
) {
}
