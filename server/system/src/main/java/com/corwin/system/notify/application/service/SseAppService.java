package com.corwin.system.notify.application.service;

import com.corwin.framework.constant.UserType;
import com.corwin.system.notify.application.event.MessageDeliveredEvent;
import com.corwin.system.notify.application.view.MsgPushPayloadView;
import com.corwin.system.notify.domain.model.MessageDelivery;
import com.corwin.system.notify.domain.model.MessageDeliveryStatus;
import com.corwin.system.notify.published.MsgType;
import com.corwin.system.notify.domain.repo.MessageDeliveryRepository;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * SSE 应用服务
 *
 * @author Corwin 2026/3/16
 */
@Service
@AllArgsConstructor
public class SseAppService {

    private final MessageDeliveryRepository messageDeliveryRepository;
    private final SseSessionManager sessionManager;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Sends missed messages to a reconnecting SSE client based on the last event ID.
     * <p>Deliveries already in ACKED status are skipped.</p>
     *
     * @param userId      the target user ID
     * @param userType    the target user type
     * @param lastEventId the ID of the last successfully received event (may be null)
     */
    @Transactional
    public void catchUp(Long userId, UserType userType, @Nullable Long lastEventId) {
        long afterEventId = lastEventId == null || lastEventId < 0 ? 0L : lastEventId;
        List<MessageDelivery> deliveries = new ArrayList<>(
                messageDeliveryRepository.findByUserIdAndUserTypeAndIdGreaterThanOrderByIdAsc(userId, userType,
                        afterEventId));

        for (MessageDelivery delivery : deliveries) {
            if (delivery.getStatus() == MessageDeliveryStatus.ACKED) {
                continue;
            }
            String eventId = String.valueOf(delivery.getId());
            String notificationId = delivery.getNotificationId() == null ? "" : String.valueOf(
                    delivery.getNotificationId());
            MsgPushPayloadView payload = new MsgPushPayloadView(eventId, notificationId, delivery.getMsgType(),
                    delivery.getTitle(), delivery.getContent(), delivery.getRoute(), delivery.getPriority(),
                    delivery.isPanelAutoOpen(), delivery.isOsNotificationEnabled());
            boolean delivered = sessionManager.send(userId, userType, resolveEventName(delivery.getMsgType()), payload,
                    eventId);
            if (!delivered) {
                continue;
            }
            if (delivery.getStatus() == MessageDeliveryStatus.PENDING) {
                delivery.markSent();
                messageDeliveryRepository.save(delivery);
                eventPublisher.publishEvent(
                        new MessageDeliveredEvent(delivery.getId(), delivery.getUserId(), delivery.getMsgType(),
                                delivery.getBizType(), delivery.getBizId()));
            }
        }
    }

    private String resolveEventName(String msgType) {
        MsgType type;
        try {
            type = MsgType.valueOf(msgType);
        } catch (Exception ex) {
            return msgType;
        }
        return type.name();
    }
}
