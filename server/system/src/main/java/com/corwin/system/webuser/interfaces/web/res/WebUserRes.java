package com.corwin.system.webuser.interfaces.web.res;

import com.corwin.framework.constant.UserType;

import java.time.Instant;

/**
 * Response DTO for external user information exposed to admin API.
 *
 * @author Corwin 2026/5/11
 */
public record WebUserRes(
        String id,
        String account,
        String nickname,
        UserType userType,
        String status,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt
) {
}
