package com.corwin.system.notify.application.view;

import java.time.Instant;

/**
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
        boolean read
) {
}
