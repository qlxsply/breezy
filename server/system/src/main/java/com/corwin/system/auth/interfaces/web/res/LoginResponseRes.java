package com.corwin.system.auth.interfaces.web.res;

/**
 * Response DTO for a successful login operation.
 *
 * @author Corwin 2026/1/22
 */
public record LoginResponseRes(
    String token,
    String refreshToken,
    String accessTokenExpiresAt,
    String refreshTokenExpiresAt,
    AuthUserRes user) {}
