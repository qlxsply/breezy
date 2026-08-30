package com.corwin.system.notify.application.view;

import java.time.Instant;
import java.util.List;

/**
 * Push 健康检查视图。
 *
 * @author Corwin 2026/3/21
 */
public record PushHealthView(
    boolean vapidReady,
    String vapidSubject,
    int activeSubscriptionCount,
    int inactiveSubscriptionCount,
    Instant latestSubscriptionUpdatedAt,
    Instant latestSubscriptionPushAt,
    String latestSubscriptionError,
    List<PushHealthSubscriptionView> subscriptions,
    PushHealthDeliveryView latestDelivery) {}
