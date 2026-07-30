package com.corwin.framework.event.durable.store;

/**
 * Persisted event record for durable event store.
 *
 * @author Corwin 2026/4/12
 */
public record PersistedEvent(
        String id,
        String eventType,
        String source,
        String payloadCodec,
        String payloadType,
        String payloadBody,
        long occurredAt,
        long deliverAt,
        EventStatus status,
        int subscriberCount,
        int completedCount,
        long createdAt,
        long updatedAt,
        Long completedAt,
        long version
) {
}

