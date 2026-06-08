package com.corwin.framework.scheduling;

import com.corwin.framework.constant.UserType;

import java.util.Objects;

/**
 * 定时任务执行身份。
 *
 * @author Corwin 2026/3/23
 */
public record SchedulerExecutionIdentity(
        Long userId,
        String username,
        UserType userType
) {
    public SchedulerExecutionIdentity {
        Objects.requireNonNull(userId, "userId required");
        Objects.requireNonNull(username, "username required");
        Objects.requireNonNull(userType, "userType required");
    }
}
