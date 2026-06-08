package com.corwin.system.notify.interfaces.web.req;

import jakarta.validation.constraints.NotBlank;

/**
 * 保存 Push 订阅请求。
 *
 * @author Corwin 2026/3/19
 */
public record SavePushSubscriptionReq(
        @NotBlank
        String deviceId,
        @NotBlank
        String endpoint,
        @NotBlank
        String p256dh,
        @NotBlank
        String auth
) {
}
