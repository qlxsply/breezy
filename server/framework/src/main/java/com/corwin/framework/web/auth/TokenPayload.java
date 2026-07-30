package com.corwin.framework.web.auth;

import com.corwin.framework.constant.UserType;

/**
 * Payload carried in a JWT or authentication token.
 *
 * @param userId      the authenticated user's ID
 * @param userAccount the authenticated user's account name
 * @param userType    the user's type (e.g. ADMIN, USER, GUEST)
 * @author Corwin 2026/2/14
 */
public record TokenPayload(
        Long userId,
        String userAccount,
        UserType userType
) {
}
