package com.corwin.framework.event.transport.inmemory.durable;

/**
 * Reference to a delivery task in the durable in-memory queue.
 *
 * @author Corwin 2026/4/12
 */
public record DeliveryTaskRef(
        String deliveryId,
        String eventId,
        String subscriberId,
        String consumerGroup,
        long deliverAtMillis,
        long deliveryVersion,
        long sequence
) {
}

