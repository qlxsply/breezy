package com.corwin.system.notify.interfaces.web.res;

import java.util.List;

/**
 * @author Corwin 2026/3/30
 */
public record NotificationPullRes(
        List<NotificationRes> items,
        long lastPullAt
) {
}
