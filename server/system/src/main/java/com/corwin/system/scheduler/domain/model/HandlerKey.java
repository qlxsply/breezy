package com.corwin.system.scheduler.domain.model;

import java.util.Objects;

/**
 * 任务处理器稳定标识。
 *
 * @author Corwin 2026/4/15
 */
public record HandlerKey(String value) {

    public HandlerKey {
        value = Objects.requireNonNull(value, "value required").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("value required");
        }
    }
}
