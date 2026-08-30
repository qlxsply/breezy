package com.corwin.system.notify.application.view;

import java.time.Instant;

/**
 * Push 订阅健康状态视图。
 *
 * @author Corwin 2026/3/21
 */
public record PushHealthSubscriptionView(
    String deviceId,
    boolean active,
    String endpointHost,
    Instant updatedAt,
    Instant lastPushAt,
    String lastError) {}
