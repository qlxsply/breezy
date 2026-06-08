package com.corwin.system.user.interfaces.web.res;

import com.corwin.framework.constant.UserType;
import com.corwin.system.user.domain.model.UserStatus;

import java.time.Instant;

/**
 * @author Corwin 2026/1/22
 */
public record UserRes(
        String id,
        String username,
        String nickname,
        UserType userType,
        UserStatus status,
        boolean deletedFlag,
        String createdBy,
        Instant createdAt,
        String updatedBy,
        Instant updatedAt
) {
}
