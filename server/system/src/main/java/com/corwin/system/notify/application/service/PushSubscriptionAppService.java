package com.corwin.system.notify.application.service;

import com.corwin.framework.constant.UserType;
import com.corwin.system.notify.application.command.SavePushSubscriptionCommand;
import com.corwin.system.notify.domain.model.UserPushSubscription;
import com.corwin.system.notify.domain.repo.UserPushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Push 订阅应用服务。
 *
 * @author Corwin 2026/3/19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushSubscriptionAppService {

    private final UserPushSubscriptionRepository userPushSubscriptionRepository;
    private final WebPushVapidService webPushVapidService;

    public String getPublicKey() {
        return webPushVapidService.getPublicKey();
    }

    @Transactional
    public void saveSubscription(Long userId, UserType userType, SavePushSubscriptionCommand command) {
        if (userId == null || userType == null || command == null) {
            log.warn("Skip save push subscription because userId/userType/command is null");
            return;
        }

        log.info("Saving push subscription: userType={}, userId={}, deviceId={}", userType, userId,
                command.deviceId());

        userPushSubscriptionRepository.findByUserIdAndUserTypeAndDeviceId(userId, userType, command.deviceId())
                .ifPresentOrElse(existing -> {
                    existing.refresh(command.endpoint(), command.p256dh(), command.auth());
                    userPushSubscriptionRepository.save(existing);
                    log.info("Push subscription refreshed: userType={}, userId={}, deviceId={}", userType, userId,
                            command.deviceId());
                }, () -> {
                    UserPushSubscription subscription = UserPushSubscription.register(userId, userType,
                            command.deviceId(), command.endpoint(), command.p256dh(), command.auth());
                    userPushSubscriptionRepository.save(subscription);
                    log.info("Push subscription created: userType={}, userId={}, deviceId={}", userType, userId,
                            command.deviceId());
                });
    }

    @Transactional
    public void removeSubscription(Long userId, UserType userType, String deviceId) {
        if (userId == null || userType == null) {
            log.warn("Skip remove push subscription because userId/userType is null");
            return;
        }
        if (deviceId == null || deviceId.isBlank()) {
            log.warn("Skip remove push subscription because deviceId is blank, userType={}, userId={}", userType,
                    userId);
            return;
        }

        String normalizedDeviceId = deviceId.trim();
        log.info("Removing push subscription: userType={}, userId={}, deviceId={}", userType, userId,
                normalizedDeviceId);

        userPushSubscriptionRepository.findByUserIdAndUserTypeAndDeviceId(userId, userType, normalizedDeviceId)
                .ifPresent(subscription -> {
                    subscription.deactivate("用户主动取消订阅");
                    userPushSubscriptionRepository.save(subscription);
                    log.info("Push subscription deactivated: userType={}, userId={}, deviceId={}", userType,
                            userId, normalizedDeviceId);
                });
    }
}
