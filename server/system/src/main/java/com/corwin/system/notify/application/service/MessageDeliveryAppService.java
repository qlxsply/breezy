package com.corwin.system.notify.application.service;

import com.corwin.framework.constant.UserType;
import com.corwin.system.notify.application.port.MessageDeliveryPort;
import com.corwin.system.notify.domain.model.MessageDeliveryStatus;
import com.corwin.system.notify.domain.repo.MessageDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service implementing {@link MessageDeliveryPort} for acknowledging deliveries.
 *
 * @author Corwin 2026/4/15
 */
@Service
@RequiredArgsConstructor
public class MessageDeliveryAppService implements MessageDeliveryPort {

    private final MessageDeliveryRepository messageDeliveryRepository;

    /**
     * Acknowledges a delivery as received/read by the user, if not already acknowledged.
     *
     * @param userId     the user ID
     * @param userType   the user type
     * @param deliveryId the delivery record ID
     */
    @Override
    @Transactional
    public void ackDelivery(Long userId, UserType userType, Long deliveryId) {
        messageDeliveryRepository.findByIdAndUserIdAndUserType(deliveryId, userId, userType).ifPresent(delivery -> {
            if (delivery.getStatus() != MessageDeliveryStatus.ACKED) {
                delivery.markAcked();
                messageDeliveryRepository.save(delivery);
            }
        });
    }
}
