package com.corwin.system.methodstat.domain.model;

import com.corwin.framework.event.model.AsyncEvent;

import java.util.Objects;

/**
 * @author Corwin 2026/3/25
 */
public record MethodStatInvocationEvent(
        MethodStatKey key,
        long startedAtMillis,
        long finishedAtMillis,
        long durationMillis,
        boolean success,
        String exceptionClassName
) implements AsyncEvent {

    public MethodStatInvocationEvent {
        Objects.requireNonNull(key, "key required");
        if (durationMillis < 0) {
            durationMillis = 0L;
        }
    }
}
