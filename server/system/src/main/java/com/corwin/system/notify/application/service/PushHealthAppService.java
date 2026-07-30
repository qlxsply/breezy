package com.corwin.system.notify.application.service;

import com.corwin.framework.config.ConfigRegistry;
import com.corwin.framework.constant.UserType;
import com.corwin.system.notify.application.view.PushHealthDeliveryView;
import com.corwin.system.notify.application.view.PushHealthSubscriptionView;
import com.corwin.system.notify.application.view.PushHealthView;
import com.corwin.system.config.application.config.SystemConfigKeys;
import com.corwin.system.notify.domain.model.MessageDelivery;
import com.corwin.system.notify.published.MsgType;
import com.corwin.system.notify.domain.model.UserPushSubscription;
import com.corwin.system.notify.domain.repo.MessageDeliveryRepository;
import com.corwin.system.notify.domain.repo.UserPushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Push 健康检查应用服务。
 *
 * @author Corwin 2026/3/21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushHealthAppService {

    private final UserPushSubscriptionRepository userPushSubscriptionRepository;
    private final MessageDeliveryRepository messageDeliveryRepository;
    private final NotificationDispatcher notificationDispatcher;
    private final WebPushVapidService webPushVapidService;

    /**
     * Queries the push health status for the specified user, including subscription
     * details, delivery info, and VAPID readiness.
     *
     * @param userId   the user ID
     * @param userType the user type
     * @return the push health view
     */
    @Transactional(readOnly = true)
    public PushHealthView queryHealth(Long userId, UserType userType) {
        if (userId == null || userType == null) {
            return new PushHealthView(false, "", 0, 0, null, null, "", List.of(), null);
        }

        webPushVapidService.ensureVapidKeys();

        List<UserPushSubscription> subscriptions = userPushSubscriptionRepository
                .findByUserIdAndUserTypeOrderByUpdatedAtDesc(userId, userType);
        int activeCount = (int) subscriptions.stream().filter(UserPushSubscription::isActive).count();
        int inactiveCount = Math.max(0, subscriptions.size() - activeCount);

        Instant latestUpdatedAt = subscriptions.stream().map(UserPushSubscription::getUpdatedAt)
                .filter(it -> it != null).max(Comparator.naturalOrder()).orElse(null);
        Instant latestPushAt = subscriptions.stream().map(UserPushSubscription::getLastPushAt).filter(it -> it != null)
                .max(Comparator.naturalOrder()).orElse(null);
        String latestError = subscriptions.stream().map(UserPushSubscription::getLastError)
                .filter(it -> it != null && !it.isBlank()).findFirst().orElse("");

        List<PushHealthSubscriptionView> subscriptionViews = subscriptions.stream().map(this::toSubscriptionView)
                .toList();

        PushHealthDeliveryView latestDelivery = messageDeliveryRepository
                .findFirstByUserIdAndUserTypeOrderByIdDesc(userId, userType).map(this::toDeliveryView).orElse(null);

        String vapidSubject = ConfigRegistry.stringV(SystemConfigKeys.WEB_PUSH_VAPID_SUBJECT);
        boolean vapidReady = !isBlank(ConfigRegistry.stringV(SystemConfigKeys.WEB_PUSH_VAPID_PUBLIC_KEY)) && !isBlank(
                ConfigRegistry.stringV(SystemConfigKeys.WEB_PUSH_VAPID_PRIVATE_KEY));

        return new PushHealthView(vapidReady, vapidSubject, activeCount, inactiveCount, latestUpdatedAt, latestPushAt,
                latestError, subscriptionViews, latestDelivery);
    }

    /**
     * Sends a high-priority health check notification to verify the push delivery chain.
     *
     * @param userId   the user ID
     * @param userType the user type
     * @return the delivery view of the sent health check
     */
    @Transactional
    public PushHealthDeliveryView sendHighPriorityHealthCheck(Long userId, UserType userType) {
        if (userId == null || userType == null) {
            return null;
        }

        MessageDelivery delivery = notificationDispatcher.dispatchPreview(userId, userType, MsgType.SYSTEM_EVENT,
                "[健康检查] 高优先级系统通知测试", "如果你收到了这条系统通知，说明 Web Push 投递链路正常。", "/todo/all",
                "HIGH", false, true, true, true);
        log.info("Push health test message dispatched: userType={}, userId={}, deliveryId={}", userType, userId,
                delivery.getId());
        return toDeliveryView(delivery);
    }

    private PushHealthSubscriptionView toSubscriptionView(UserPushSubscription subscription) {
        return new PushHealthSubscriptionView(subscription.getDeviceId(), subscription.isActive(),
                resolveEndpointHost(subscription.getEndpoint()), subscription.getUpdatedAt(),
                subscription.getLastPushAt(), subscription.getLastError());
    }

    private PushHealthDeliveryView toDeliveryView(MessageDelivery delivery) {
        return new PushHealthDeliveryView(String.valueOf(delivery.getId()), delivery.getMsgType(),
                delivery.getPriority(), delivery.getStatus().name(), delivery.getRoute(), delivery.getCreatedAt(),
                delivery.getSentAt(), delivery.getAckedAt());
    }

    private String resolveEndpointHost(String endpoint) {
        if (isBlank(endpoint)) {
            return "";
        }
        try {
            return URI.create(endpoint).getHost();
        } catch (Exception ex) {
            return "";
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
