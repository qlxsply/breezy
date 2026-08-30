package com.corwin.system.notify.interfaces.web.res;

import java.util.List;

/**
 * Response DTO for pull-based notification retrieval.
 *
 * @param items the list of notification records
 * @param lastPullAt the timestamp (epoch millis) of the last pull, for incremental fetching
 * @author Corwin 2026/3/30
 */
public record NotificationPullRes(List<NotificationRes> items, long lastPullAt) {}
