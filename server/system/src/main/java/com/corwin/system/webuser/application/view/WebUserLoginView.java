package com.corwin.system.webuser.application.view;

/**
 * View returned upon successful external user login, containing JWT tokens and user info.
 *
 * @param token the issued access token
 * @param refreshToken the refresh token for obtaining a new access token
 * @param accessTokenExpiresAt epoch milliseconds when the access token expires
 * @param refreshTokenExpiresAt epoch milliseconds when the refresh token expires
 * @param user the authenticated user summary
 * @author Corwin 2026/5/11
 */
public record WebUserLoginView(
    String token,
    String refreshToken,
    String accessTokenExpiresAt,
    String refreshTokenExpiresAt,
    WebUserAuthView user) {}
