package com.corwin.system.methodstat.domain.model;

import java.util.Objects;

/**
 * @author Corwin 2026/3/25
 */
public record MethodStatKey(String value) {

    public MethodStatKey {
        Objects.requireNonNull(value, "value required");
        value = value.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("value required");
        }
    }

    public static MethodStatKey of(String value) {
        return new MethodStatKey(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
