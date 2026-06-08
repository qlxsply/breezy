package com.corwin.system.notify.application.view;

/**
 * SSE 推送消息载荷。
 *
 * @author Corwin 2026/3/19
 */
public record MsgPushPayloadView(
        String eventId,
        String notificationId,
        String msgType,
        String title,
        String content,
        String route,
        String priority,
        boolean panelAutoOpen,
        boolean osNotificationEnabled
) {
}
