package com.corwin.system.notify.interfaces.web.res;

import java.time.Instant;
import java.util.List;

/**
 * Push 健康检查响应。
 *
 * @author Corwin 2026/3/21
 */
public record PushHealthRes(
        boolean vapidReady,
        String vapidSubject,
        int activeSubscriptionCount,
        int inactiveSubscriptionCount,
        Instant latestSubscriptionUpdatedAt,
        Instant latestSubscriptionPushAt,
        String latestSubscriptionError,
        List<PushHealthSubscriptionRes> subscriptions,
        PushHealthDeliveryRes latestDelivery
) {
}
