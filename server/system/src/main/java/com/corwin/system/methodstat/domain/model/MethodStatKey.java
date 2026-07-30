package com.corwin.system.methodstat.domain.model;

import java.util.Objects;

/**
 * Domain value object representing a unique key identifying a monitored method,
 * typically derived from its canonical method signature.
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

    /**
     * Create a MethodStatKey from the given string value.
     * @param value the key string
     * @return a new MethodStatKey instance
     */
    public static MethodStatKey of(String value) {
        return new MethodStatKey(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
