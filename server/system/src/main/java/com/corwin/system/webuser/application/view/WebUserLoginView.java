package com.corwin.system.webuser.application.view;

/**
 * @author Corwin 2026/5/11
 */
public record WebUserLoginView(
        String token,
        String refreshToken,
        String accessTokenExpiresAt,
        String refreshTokenExpiresAt,
        WebUserAuthView user
) {
}
