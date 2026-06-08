package com.corwin.framework.event.transport.inmemory.durable;

/**
 * durable 内存队列中的投递任务引用。
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

