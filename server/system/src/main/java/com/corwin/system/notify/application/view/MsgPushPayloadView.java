package com.corwin.system.notify.application.view;

/**
 * Payload view for SSE-pushed messages sent to the client in real-time.
 *
 * @param eventId the delivery event ID
 * @param notificationId the associated notification ID
 * @param msgType the message type
 * @param title the message title
 * @param content the message content
 * @param route the front-end route
 * @param priority the priority level
 * @param panelAutoOpen whether to auto-open the notification panel
 * @param osNotificationEnabled whether to send OS-level notification
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
    boolean osNotificationEnabled) {}
