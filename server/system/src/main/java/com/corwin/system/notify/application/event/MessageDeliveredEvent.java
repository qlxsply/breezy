package com.corwin.system.notify.application.event;

/**
 * 通用消息投递成功事件。
 *
 * @author Corwin 2026/4/7
 */
public record MessageDeliveredEvent(
        Long deliveryId,
        Long userId,
        String msgType,
        String bizType,
        String bizId
) {
}
