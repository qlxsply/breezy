package com.corwin.system.user.interfaces.web.req;

import com.corwin.system.user.domain.model.UserStatus;

import java.util.List;

/**
 * @author Corwin 2026/7/7
 */
public record BatchUpdateUserStatusReq(
        List<Long> userIds,
        UserStatus status
) {
}
