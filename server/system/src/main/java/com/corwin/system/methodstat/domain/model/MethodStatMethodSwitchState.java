package com.corwin.system.methodstat.domain.model;

import java.util.Objects;

/**
 * @author Corwin 2026/3/25
 */
public record MethodStatMethodSwitchState(
        MethodStatKey key,
        boolean enabled
) {

    public MethodStatMethodSwitchState {
        Objects.requireNonNull(key, "key required");
    }
}
