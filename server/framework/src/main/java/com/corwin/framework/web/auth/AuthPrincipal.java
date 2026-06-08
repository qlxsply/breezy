package com.corwin.framework.web.auth;

import com.corwin.framework.constant.UserType;

import java.util.Set;

/**
 *
 * @author Corwin 2026/4/20
 */
public record AuthPrincipal(
        Long userId,
        String username,
        UserType userType,
        boolean admin,
        Set<String> permissionCodes
) {
    public AuthPrincipal {
        permissionCodes = permissionCodes == null ? Set.of() : Set.copyOf(permissionCodes);
        userType = userType == null ? UserType.GUEST : userType;
    }

    public static AuthPrincipal guest() {
        return new AuthPrincipal(null, null, UserType.GUEST, false, Set.of());
    }

    public boolean authenticated() {
        return userType != UserType.GUEST && userId != null;
    }
}
