package com.corwin.system.auth.interfaces.web.res;

/**
 * @author Corwin 2026/8/20
 */
public record AdminLoginResponseRes(
        String sessionExpiresAt,
        AuthUserRes user
) {
}
