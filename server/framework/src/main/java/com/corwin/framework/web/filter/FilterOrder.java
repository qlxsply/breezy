package com.corwin.framework.web.filter;

import org.springframework.core.Ordered;

/**
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
