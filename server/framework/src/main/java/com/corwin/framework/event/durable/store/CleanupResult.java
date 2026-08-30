package com.corwin.framework.event.durable.store;

/**
 * Result of a durable store cleanup operation.
 *
 * @author Corwin 2026/4/12
 */
public record CleanupResult(int deletedDeliveries, int deletedEvents) {}
