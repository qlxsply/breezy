package com.corwin.system.notify.application.event;

/**
 * Event published when a message has been successfully delivered.
 *
 * @param deliveryId the delivery record ID
 * @param userId     the target user ID
 * @param msgType    the message type
 * @param bizType    the business type (optional)
 * @param bizId      the business ID (optional)
 * @author Corwin 2026/4/7
 */
public record MessageDeliveredEvent(
        Long deliveryId,
        Long userId,
        String msgType,
        String bizType,
        String bizId
) {
}
