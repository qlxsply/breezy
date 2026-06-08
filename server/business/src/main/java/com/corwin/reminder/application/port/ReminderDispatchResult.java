package com.corwin.reminder.application.port;

/**
 * 提醒消息分发结果。
 *
 * @author Corwin 2026/4/15
 */
public record ReminderDispatchResult(
        Long deliveryId,
        boolean delivered
) {
}
