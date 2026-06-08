package com.corwin.framework.event.durable.store;

/**
 * 清理执行结果。
 *
 * @author Corwin 2026/4/12
 */
public record CleanupResult(
        int deletedDeliveries,
        int deletedEvents
) {
}

