package com.corwin.system.notify.application.service;

import com.corwin.framework.json.Json;
import com.corwin.system.notify.domain.model.MessageDelivery;
import com.corwin.system.notify.domain.model.MessageDeliveryStatus;
import com.corwin.system.notify.domain.model.UserPushSubscription;
import com.corwin.system.notify.domain.repo.MessageDeliveryRepository;
import com.corwin.system.notify.domain.repo.UserPushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Web Push 分发服务。
 *
 * @author Corwin 2026/3/19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebPushDispatchService {

    private final UserPushSubscriptionRepository userPushSubscriptionRepository;
    private final MessageDeliveryRepository messageDeliveryRepository;
    private final WebPushVapidService webPushVapidService;

    @Async("taskExecutor")
    @Transactional
    public void dispatchAsync(Long deliveryId) {
        dispatchWithRetry(deliveryId, true);
    }

    @Scheduled(fixedDelayString = "${breezy.msg.push.retry-delay-ms:20000}")
    @Transactional
    public void retryPendingDeliveries() {
        List<MessageDelivery> pendings = messageDeliveryRepository.findTop50ByStatusOrderByIdAsc(
                MessageDeliveryStatus.PENDING);
        if (pendings.isEmpty()) {
            return;
        }

        log.info("WebPush pending retry scan: pendingCount={}", pendings.size());
        for (MessageDelivery pending : pendings) {
            dispatchWithRetry(pending.getId(), false);
        }
    }

    private void dispatchWithRetry(Long deliveryId, boolean retryVisibility) {
        MessageDelivery delivery = loadDeliveryWithRetry(deliveryId, retryVisibility);
        if (delivery == null || delivery.getStatus() == MessageDeliveryStatus.ACKED) {
            log.info("WebPush dispatch skipped: delivery missing or acked, deliveryId={}", deliveryId);
            return;
        }

        log.info("WebPush dispatch start: deliveryId={}, userId={}, msgType={}, priority={}, status={}",
                delivery.getId(), delivery.getUserId(), delivery.getMsgType(), delivery.getPriority(),
                delivery.getStatus());

        List<UserPushSubscription> subscriptions = userPushSubscriptionRepository.findByUserIdAndUserTypeAndActiveTrue(
                delivery.getUserId(), delivery.getUserType());
        if (subscriptions.isEmpty()) {
            log.info("WebPush dispatch skipped: no active subscriptions, userType={}, userId={}, deliveryId={}",
                    delivery.getUserType(), delivery.getUserId(), delivery.getId());
            if (delivery.getStatus() == MessageDeliveryStatus.PENDING) {
                delivery.markFailed();
                messageDeliveryRepository.save(delivery);
            }
            return;
        }

        String payload = toPayload(delivery);
        PushService pushService;
        try {
            pushService = webPushVapidService.newPushService();
        } catch (Exception ex) {
            log.error("Create WebPush service failed, deliveryId={}", deliveryId, ex);
            return;
        }

        boolean hasSuccess = false;
        int successCount = 0;
        int failureCount = 0;
        for (UserPushSubscription subscription : subscriptions) {
            try {
                Notification notification = new Notification(subscription.getEndpoint(), subscription.getP256dh(),
                        subscription.getAuthSecret(), payload);
                HttpResponse response = pushService.send(notification);
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    hasSuccess = true;
                    successCount += 1;
                    subscription.markPushSuccess();
                    log.info("WebPush send success: userId={}, deliveryId={}, deviceId={}, statusCode={}",
                            subscription.getUserType(), subscription.getUserId(), delivery.getId(),
                            subscription.getDeviceId(), statusCode);
                } else if (statusCode == 401 || statusCode == 403 || statusCode == 404 || statusCode == 410) {
                    failureCount += 1;
                    subscription.deactivate("Push endpoint unavailable: HTTP " + statusCode);
                    log.warn("WebPush endpoint invalidated: userId={}, deliveryId={}, deviceId={}, statusCode={}",
                            subscription.getUserType(), subscription.getUserId(), delivery.getId(),
                            subscription.getDeviceId(), statusCode);
                } else {
                    failureCount += 1;
                    subscription.markPushFailure("Push failed: HTTP " + statusCode);
                    log.warn("WebPush send failed: userId={}, deliveryId={}, deviceId={}, statusCode={}",
                            subscription.getUserType(), subscription.getUserId(), delivery.getId(),
                            subscription.getDeviceId(), statusCode);
                }
            } catch (Exception ex) {
                failureCount += 1;
                subscription.markPushFailure(truncateError(ex.getMessage()));
                log.warn("WebPush send failed, userType={}, userId={}, deliveryId={}, deviceId={}",
                        subscription.getUserType(), subscription.getUserId(), deliveryId,
                        subscription.getDeviceId(), ex);
            }
            userPushSubscriptionRepository.save(subscription);
        }

        if (hasSuccess && delivery.getStatus() == MessageDeliveryStatus.PENDING) {
            delivery.markSent();
            messageDeliveryRepository.save(delivery);
        } else if (!hasSuccess && delivery.getStatus() == MessageDeliveryStatus.PENDING) {
            delivery.markFailed();
            messageDeliveryRepository.save(delivery);
        }

        log.info("WebPush dispatch finished: deliveryId={}, successCount={}, failureCount={}, finalStatus={}",
                delivery.getId(), successCount, failureCount, delivery.getStatus());
    }

    private MessageDelivery loadDeliveryWithRetry(Long deliveryId, boolean retryVisibility) {
        if (!retryVisibility) {
            return messageDeliveryRepository.findById(deliveryId).orElse(null);
        }

        for (int attempt = 1; attempt <= 3; attempt++) {
            MessageDelivery delivery = messageDeliveryRepository.findById(deliveryId).orElse(null);
            if (delivery != null) {
                return delivery;
            }

            if (attempt < 3) {
                log.info("WebPush delivery not visible yet, wait and retry: deliveryId={}, attempt={}", deliveryId,
                        attempt);
                sleepSilently(120L);
            }
        }

        return null;
    }

    private void sleepSilently(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private String toPayload(MessageDelivery delivery) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", String.valueOf(delivery.getId()));
        payload.put("notificationId",
                delivery.getNotificationId() == null ? "" : String.valueOf(delivery.getNotificationId()));
        payload.put("msgType", delivery.getMsgType());
        payload.put("title", delivery.getTitle());
        payload.put("content", delivery.getContent());
        payload.put("route", delivery.getRoute());
        payload.put("priority", delivery.getPriority());
        payload.put("panelAutoOpen", delivery.isPanelAutoOpen());
        payload.put("osNotificationEnabled", delivery.isOsNotificationEnabled());
        return Json.toStr(payload);
    }

    private String truncateError(String message) {
        if (message == null) {
            return "Push send failed";
        }
        if (message.length() <= 800) {
            return message;
        }
        return message.substring(0, 800);
    }
}
