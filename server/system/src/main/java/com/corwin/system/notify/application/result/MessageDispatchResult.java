package com.corwin.system.notify.application.result;

/**
 * @author Corwin 2026/4/15
 */
public record MessageDispatchResult(
        Long deliveryId,
        Long notificationId,
        boolean delivered
) {
}
