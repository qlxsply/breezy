package com.corwin.framework.web.filter;

import org.springframework.core.Ordered;

/**
 * Defines the execution order of framework filters in the filter chain.
 * <p>
 * Each constant's {@link #value()} is computed as
 * {@link Ordered#HIGHEST_PRECEDENCE} + ordinal, ensuring deterministic ordering.
 *
 * @author Corwin 2025/10/23
 */
public enum FilterOrder {
    TRACE_FILTER,
    LOGGING_FILTER,
    AUTH_FILTER,
    ;

    public int value() {
        return Ordered.HIGHEST_PRECEDENCE + this.ordinal();
    }
}
