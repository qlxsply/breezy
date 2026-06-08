package com.corwin.framework.web.auth;

import com.corwin.framework.constant.UserType;

/**
 * @author Corwin 2026/2/14
 */
public record TokenPayload(
        Long userId,
        String userAccount,
        UserType userType
) {
}
