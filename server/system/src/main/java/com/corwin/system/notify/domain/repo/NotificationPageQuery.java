package com.corwin.system.notify.domain.repo;

import com.corwin.framework.constant.UserType;

/**
 * 通知分页查询条件。
 *
 * @author Corwin 2026/4/15
 */
public record NotificationPageQuery(
        Long userId,
        UserType userType,
        boolean unreadOnly
) {
}
