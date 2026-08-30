package com.corwin.system.notify.application.view;

import java.time.Instant;

/**
 * View object representing a notification for the application layer.
 *
 * @param id the notification ID
 * @param title the notification title
 * @param content the notification content
 * @param type the message type
 * @param priority the priority level
 * @param route the front-end route
 * @param createdAt the creation timestamp
 * @param read whether the notification has been read
 * @author Corwin 2026/3/30
 */
public record NotificationView(
    String id,
    String title,
    String content,
    String type,
    String priority,
    String route,
    Instant createdAt,
    boolean read) {}
