package com.corwin.system.user.interfaces.web.res;

import com.corwin.framework.constant.UserType;

import java.time.Instant;

/**
 * Response DTO for an end-user profile.
 *
 * @author Corwin 2026/4/19
 */
public record UserProfileRes(
        String id,
        String username,
        String nickname,
        UserType userType,
        String status,
        boolean mustChangePassword,
        Instant createdAt,
        Instant updatedAt
) {
}
