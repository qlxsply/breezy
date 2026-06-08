package com.corwin.framework.event.durable.store;

/**
 * durable 投递持久化模型。
 *
 * @author Corwin 2026/4/12
 */
public record PersistedDelivery(
        String id,
        String eventId,
        String subscriberId,
        String consumerGroup,
        long deliverAt,
        DeliveryStatus status,
        String ownerNode,
        Long claimUntil,
        int attemptCount,
        String lastError,
        long createdAt,
        long updatedAt,
        Long completedAt,
        long version
) {
}

