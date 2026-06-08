package com.corwin.system.notify.application.port;

import com.corwin.framework.constant.UserType;

/**
 * @author Corwin 2026/4/15
 */
public interface MessageDeliveryPort {

    void ackDelivery(Long userId, UserType userType, Long deliveryId);
}
