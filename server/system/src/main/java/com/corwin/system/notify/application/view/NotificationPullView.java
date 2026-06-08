package com.corwin.system.notify.application.view;

import java.util.List;

/**
 * @author Corwin 2026/3/30
 */
public record NotificationPullView(
        List<NotificationView> items,
        long lastPullAt
) {
}
