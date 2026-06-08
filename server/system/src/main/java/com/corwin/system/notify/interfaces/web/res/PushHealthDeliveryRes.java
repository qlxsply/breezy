package com.corwin.system.notify.interfaces.web.res;

import java.time.Instant;

/**
 * Push 投递健康状态响应。
 *
 * @author Corwin 2026/3/21
 */
public record PushHealthDeliveryRes(
        String id,
        String msgType,
        String priority,
        String status,
        String route,
        Instant createdAt,
        Instant sentAt,
        Instant ackedAt
) {
}
