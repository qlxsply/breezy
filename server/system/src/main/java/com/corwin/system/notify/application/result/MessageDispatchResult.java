package com.corwin.system.notify.application.result;

/**
 * Result record returned after a message dispatch operation.
 *
 * @param deliveryId the ID of the created delivery record
 * @param notificationId the ID of the associated notification (may be null)
 * @param delivered whether the message was successfully delivered in real-time
 * @author Corwin 2026/4/15
 */
public record MessageDispatchResult(Long deliveryId, Long notificationId, boolean delivered) {}
