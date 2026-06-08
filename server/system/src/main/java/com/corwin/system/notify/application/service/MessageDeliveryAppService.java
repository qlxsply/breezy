package com.corwin.system.notify.application.service;

import com.corwin.framework.constant.UserType;
import com.corwin.system.notify.application.port.MessageDeliveryPort;
import com.corwin.system.notify.domain.model.MessageDeliveryStatus;
import com.corwin.system.notify.domain.repo.MessageDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Corwin 2026/4/15
 */
@Service
@RequiredArgsConstructor
public class MessageDeliveryAppService implements MessageDeliveryPort {

    private final MessageDeliveryRepository messageDeliveryRepository;

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
