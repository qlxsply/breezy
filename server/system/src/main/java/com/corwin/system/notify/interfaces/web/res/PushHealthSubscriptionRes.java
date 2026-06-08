package com.corwin.system.notify.interfaces.web.res;

import java.time.Instant;

/**
 * Push 订阅健康状态响应。
 *
 * @author Corwin 2026/3/21
 */
public record PushHealthSubscriptionRes(
        String deviceId,
        boolean active,
        String endpointHost,
        Instant updatedAt,
        Instant lastPushAt,
        String lastError
) {
}
