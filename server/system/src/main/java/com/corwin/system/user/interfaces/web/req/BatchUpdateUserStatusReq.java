package com.corwin.system.user.interfaces.web.req;

import com.corwin.system.user.domain.model.UserStatus;

import java.util.List;

/**
 * Request object for batch updating the status of multiple users.
 *
 * @author Corwin 2026/7/7
 */
public record BatchUpdateUserStatusReq(
        List<Long> userIds,
        UserStatus status
) {
}
