package com.corwin.system.notify.application.port;

import com.corwin.framework.constant.UserType;

/**
 * Outbound port for acknowledging message deliveries.
 *
 * @author Corwin 2026/4/15
 */
public interface MessageDeliveryPort {

    /**
     * Acknowledges a message delivery as received/read by the user.
     *
     * @param userId     the user ID
     * @param userType   the user type
     * @param deliveryId the delivery record ID
     */
    void ackDelivery(Long userId, UserType userType, Long deliveryId);
}
