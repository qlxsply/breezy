package com.corwin.system.notify.application.view;

import java.util.List;

/**
 * View object containing a list of notification items and the last pull timestamp.
 *
 * @param items     the list of notification views
 * @param lastPullAt the timestamp (epoch millis) for the last pull
 * @author Corwin 2026/3/30
 */
public record NotificationPullView(
        List<NotificationView> items,
        long lastPullAt
) {
}
