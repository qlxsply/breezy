package com.corwin.system.user.interfaces.web.res;

import com.corwin.framework.constant.UserType;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for an admin user profile including recent login activities.
 *
 * @author Corwin 2026/6/4
 */
public record AdminProfileRes(
        String id,
        String username,
        String nickname,
        UserType userType,
        String status,
        boolean mustChangePassword,
        Instant lastPasswordChangedAt,
        Instant createdAt,
        Instant updatedAt,
        List<AdminProfileLoginActivityRes> recentActivities
) {
}
