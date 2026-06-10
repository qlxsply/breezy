package com.corwin.system.auth.application.view;

/**
 * @author Corwin 2026/1/22
 */
public record LoginView(
        String token,
        String refreshToken,
        String accessTokenExpiresAt,
        String refreshTokenExpiresAt,
        AuthUserView user
) {
}
