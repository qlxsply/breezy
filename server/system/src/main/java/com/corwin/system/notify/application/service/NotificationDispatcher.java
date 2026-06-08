package com.corwin.system.notify.application.service;

import com.corwin.framework.config.ConfigRegistry;
import com.corwin.framework.constant.UserType;
import com.corwin.framework.json.Json;
import com.corwin.system.config.application.config.SystemConfigKeys;
import com.corwin.system.notify.published.MsgType;
import com.corwin.system.notify.application.port.MessageDispatchPort;
import com.corwin.system.notify.application.result.MessageDispatchResult;
import com.corwin.system.notify.application.view.MsgPushPayloadView;
import com.corwin.system.notify.domain.model.MessageDelivery;
import com.corwin.system.notify.domain.model.MsgPriority;
import com.corwin.system.notify.domain.model.Notification;
import com.corwin.system.notify.domain.repo.MessageDeliveryRepository;
import com.corwin.system.notify.domain.repo.NotificationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * @author Corwin 2026/3/16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatcher implements MessageDispatchPort {

    private final SseSessionManager sessionManager;
    private final NotificationRepository notificationRepository;
    private final MessageDeliveryRepository messageDeliveryRepository;
    private final WebPushDispatchService webPushDispatchService;

    @Override
    @Transactional
    public MessageDispatchResult dispatch(Long userId, UserType userType, MsgType type, String title, String content,
            String fallbackRoute) {
        return dispatch(userId, userType, type, title, content, fallbackRoute, null, null);
    }

    @Override
    @Transactional
    public MessageDispatchResult dispatch(Long userId, UserType userType, MsgType type, String title, String content,
            String fallbackRoute, String bizType, String bizId) {
        ResolvedMsgBehavior behavior = resolveBehavior(type, fallbackRoute);
        DispatchOutcome outcome = dispatchWithBehavior(userId, userType, type, title, content, behavior, bizType,
                bizId);
        return toResult(outcome.delivery(), outcome.notificationId());
    }

    @Transactional
    public MessageDelivery dispatchPreview(Long userId, UserType userType, MsgType type, String title, String content,
            String route,
            String priority, Boolean sseEnabled, Boolean webPushEnabled, Boolean panelAutoOpen,
            Boolean osNotificationEnabled) {
        ResolvedMsgBehavior behavior = resolvePreviewBehavior(type, route, priority, sseEnabled, webPushEnabled,
                panelAutoOpen, osNotificationEnabled);
        return dispatchWithBehavior(userId, userType, type, title, content, behavior, null, null).delivery();
    }

    private DispatchOutcome dispatchWithBehavior(Long userId, UserType userType, MsgType type, String title,
            String content,
            ResolvedMsgBehavior behavior, String bizType, String bizId) {

        Notification notification = new Notification(userId, userType, title, content, type.name(),
                behavior.priority().name(), behavior.route());
        notification = notificationRepository.save(notification);

        MessageDelivery delivery = MessageDelivery.pending(userId, userType, notification.getId(), type.name(), title,
                content, behavior.route(), behavior.priority().name(), behavior.panelAutoOpen(),
                behavior.osNotificationEnabled(), bizType, bizId);
        delivery = messageDeliveryRepository.save(delivery);

        log.info(
                "Dispatch message prepared: userType={}, userId={}, msgType={}, deliveryId={}, notificationId={}, priority={}, route={}, sseEnabled={}, webPushEnabled={}, panelAutoOpen={}, osNotificationEnabled={}",
                userType, userId, type.name(), delivery.getId(), notification.getId(), behavior.priority().name(),
                behavior.route(), behavior.sseEnabled(), behavior.webPushEnabled(), behavior.panelAutoOpen(),
                behavior.osNotificationEnabled());

        if (behavior.sseEnabled()) {
            MsgPushPayloadView payload = new MsgPushPayloadView(String.valueOf(delivery.getId()),
                    String.valueOf(notification.getId()), type.name(), title, content, behavior.route(),
                    behavior.priority().name(), behavior.panelAutoOpen(), behavior.osNotificationEnabled());
            boolean delivered = sessionManager.send(userId, userType, type.name(), payload, payload.eventId());
            if (delivered) {
                delivery.markSent();
                messageDeliveryRepository.save(delivery);
                log.info("SSE dispatched successfully: userType={}, userId={}, deliveryId={}, eventId={}", userType,
                        userId, delivery.getId(), payload.eventId());
            } else {
                log.info("SSE skipped because no active session: userType={}, userId={}, deliveryId={}", userType,
                        userId, delivery.getId());
            }
        }

        if (behavior.webPushEnabled()) {
            log.info("Web Push dispatch queued: userType={}, userId={}, deliveryId={}", userType, userId,
                    delivery.getId());
            dispatchWebPushAfterCommit(userId, userType, delivery.getId());
        }
        return new DispatchOutcome(notification.getId(), delivery);
    }

    private MessageDispatchResult toResult(MessageDelivery delivery, Long notificationId) {
        boolean delivered = delivery.getStatus() == com.corwin.system.notify.domain.model.MessageDeliveryStatus.SENT;
        return new MessageDispatchResult(delivery.getId(), notificationId, delivered);
    }

    private void dispatchWebPushAfterCommit(Long userId, UserType userType, Long deliveryId) {
        if (deliveryId == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            log.info("Web Push dispatch trigger immediately (no tx sync): userType={}, userId={}, deliveryId={}",
                    userType, userId, deliveryId);
            webPushDispatchService.dispatchAsync(deliveryId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.info("Web Push dispatch trigger after commit: userType={}, userId={}, deliveryId={}", userType,
                        userId, deliveryId);
                webPushDispatchService.dispatchAsync(deliveryId);
            }
        });
    }

    private ResolvedMsgBehavior resolvePreviewBehavior(MsgType type, String route, String priority, Boolean sseEnabled,
            Boolean webPushEnabled, Boolean panelAutoOpen, Boolean osNotificationEnabled) {
        ResolvedMsgBehavior baseBehavior = resolveBehavior(type, route);
        MsgPriority resolvedPriority = resolvePriorityOrDefault(priority, baseBehavior.priority());
        String resolvedRoute = normalizeRoute(route, baseBehavior.route());
        boolean hasPriorityOverride = priority != null && !priority.isBlank();

        boolean resolvedSseEnabled = sseEnabled != null ? sseEnabled : baseBehavior.sseEnabled();
        boolean resolvedWebPushEnabled = webPushEnabled != null ? webPushEnabled : (hasPriorityOverride ? resolvedPriority == MsgPriority.HIGH : baseBehavior.webPushEnabled());
        boolean resolvedPanelAutoOpen = panelAutoOpen != null ? panelAutoOpen : (hasPriorityOverride ? resolvedPriority != MsgPriority.LOW : baseBehavior.panelAutoOpen());
        boolean resolvedOsNotificationEnabled = osNotificationEnabled != null ? osNotificationEnabled : (hasPriorityOverride ? resolvedPriority == MsgPriority.HIGH : baseBehavior.osNotificationEnabled());

        if (resolvedPriority == MsgPriority.HIGH) {
            resolvedWebPushEnabled = true;
            resolvedPanelAutoOpen = true;
            resolvedOsNotificationEnabled = true;
        }

        return new ResolvedMsgBehavior(resolvedRoute, resolvedPriority, resolvedSseEnabled, resolvedWebPushEnabled,
                resolvedPanelAutoOpen, resolvedOsNotificationEnabled);
    }

    private MsgPriority resolvePriorityOrDefault(String priority, MsgPriority fallback) {
        if (priority == null || priority.isBlank()) {
            return fallback;
        }
        try {
            return MsgPriority.valueOf(priority.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private String normalizeRoute(String route, String fallback) {
        String normalized = route == null ? "" : route.trim();
        if (normalized.startsWith("/")) {
            return normalized;
        }
        if (fallback != null && !fallback.isBlank() && fallback.startsWith("/")) {
            return fallback;
        }
        return "/";
    }

    private ResolvedMsgBehavior resolveBehavior(MsgType type, String fallbackRoute) {
        MsgType domainType = toDomainMsgType(type);
        MsgTypeConfigModel defaultConfig = MsgTypeConfigModel.defaultFor(domainType);
        MsgPriority priority = MsgPriority.valueOf(defaultConfig.getPriority());
        String route = defaultConfig.getRoute();
        boolean sseEnabled = true;
        boolean webPushEnabled = priority == MsgPriority.HIGH;
        boolean panelAutoOpen = priority != MsgPriority.LOW;
        boolean osNotificationEnabled = priority == MsgPriority.HIGH;

        for (MsgTypeConfigModel config : loadConfigs()) {
            MsgType resolvedType = config.resolveMsgTypeOrNull();
            if (resolvedType != domainType) {
                continue;
            }
            priority = config.resolvePriority(priority);
            route = config.normalizeRoute(domainType);
            sseEnabled = config.resolveSseEnabled(true);
            webPushEnabled = config.resolveWebPushEnabled(priority == MsgPriority.HIGH);
            panelAutoOpen = config.resolvePanelAutoOpen(priority != MsgPriority.LOW);
            osNotificationEnabled = config.resolveOsNotificationEnabled(priority == MsgPriority.HIGH);
            break;
        }

        if ((route == null || route.isBlank()) && fallbackRoute != null && !fallbackRoute.isBlank()) {
            route = fallbackRoute.trim();
        }
        if (route == null || route.isBlank() || !route.startsWith("/")) {
            route = defaultConfig.getRoute();
        }

        if (priority == MsgPriority.HIGH) {
            webPushEnabled = true;
            panelAutoOpen = true;
            osNotificationEnabled = true;
        }

        return new ResolvedMsgBehavior(route, priority, sseEnabled, webPushEnabled, panelAutoOpen,
                osNotificationEnabled);
    }

    private List<MsgTypeConfigModel> loadConfigs() {
        return ConfigRegistry.customV(SystemConfigKeys.MSG_TYPE_CONFIGS, raw -> {
            if (raw == null || raw.isBlank()) {
                return List.<MsgTypeConfigModel>of();
            }
            return Json.parse(raw, new TypeReference<>() {
            });
        });
    }

    private MsgType toDomainMsgType(MsgType type) {
        return MsgType.valueOf(type.name());
    }

    private record DispatchOutcome(
            Long notificationId,
            MessageDelivery delivery
    ) {
    }

    private record ResolvedMsgBehavior(
            String route,
            MsgPriority priority,
            boolean sseEnabled,
            boolean webPushEnabled,
            boolean panelAutoOpen,
            boolean osNotificationEnabled
    ) {
    }
}
