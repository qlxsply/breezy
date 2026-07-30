package com.corwin.system.notify.domain.repo;

import com.corwin.framework.constant.UserType;

/**
 * Query criteria for paginated notification retrieval.
 *
 * @param userId     the target user ID
 * @param userType   the target user type
 * @param unreadOnly whether to filter only unread notifications
 * @author Corwin 2026/4/15
 */
public record NotificationPageQuery(
        Long userId,
        UserType userType,
        boolean unreadOnly
) {
}
