package com.corwin.system.user.application.view;

import com.corwin.framework.constant.UserType;

import java.time.Instant;
import java.util.List;

/**
 * @author Corwin 2026/6/4
 */
public record AdminProfileView(
        Long id,
        String username,
        String nickname,
        UserType userType,
        String status,
        boolean mustChangePassword,
        Instant lastPasswordChangedAt,
        Instant createdAt,
        Instant updatedAt,
        List<AdminProfileLoginActivityView> recentActivities
) {
}
