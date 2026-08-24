package com.corwin.framework.web.auth;

import com.corwin.framework.constant.UserType;

import java.util.Set;

/**
 * Authenticated principal representing the current user.
 * <p>
 * Carries the user's identity, role type, administrative flag, and
 * permission codes. A {@link #guest()} factory produces an unauthenticated
 * principal with no permissions.
 *
 * @param userId          the user's ID, may be null for guests
 * @param username        the user's login name
 * @param userType        the user type ({@link UserType})
 * @param admin           whether the user has administrative privileges
 * @param credentialsExpired whether credentials must be updated before normal access
 * @param permissionCodes the set of permission codes granted to the user
 * @author Corwin 2026/4/20
 */
public record AuthPrincipal(
        Long userId,
        String username,
        UserType userType,
        boolean admin,
        boolean credentialsExpired,
        Set<String> permissionCodes
) {
    public AuthPrincipal {
        permissionCodes = permissionCodes == null ? Set.of() : Set.copyOf(permissionCodes);
        userType = userType == null ? UserType.GUEST : userType;
    }

    public AuthPrincipal(Long userId, String username, UserType userType, boolean admin,
            Set<String> permissionCodes) {
        this(userId, username, userType, admin, false, permissionCodes);
    }

    public static AuthPrincipal guest() {
        return new AuthPrincipal(null, null, UserType.GUEST, false, false, Set.of());
    }

    public boolean authenticated() {
        return userType != UserType.GUEST && userId != null;
    }
}
