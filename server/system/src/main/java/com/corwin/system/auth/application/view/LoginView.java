package com.corwin.system.auth.application.view;

/**
 * View object representing the result of a successful login, including access token, refresh token,
 * expiration timestamps and user details.
 *
 * @author Corwin 2026/1/22
 */
public record LoginView(
    String token,
    String refreshToken,
    String accessTokenExpiresAt,
    String refreshTokenExpiresAt,
    AuthUserView user) {}
