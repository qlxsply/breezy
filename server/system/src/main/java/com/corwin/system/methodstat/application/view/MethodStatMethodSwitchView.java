package com.corwin.system.methodstat.application.view;

/**
 * View object representing the per-method switch state (enabled/disabled) for a given key.
 * @author Corwin 2026/3/25
 */
public record MethodStatMethodSwitchView(
        String key,
        boolean enabled
) {
}
