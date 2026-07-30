package com.corwin.system.auth.interfaces.web.res;

/**
 * Response DTO for token refresh operation.
 *
 * @author Corwin 2026/6/11
 */
public record RefreshTokenResponseRes(
        String token,
        String refreshToken,
        String accessTokenExpiresAt,
        String refreshTokenExpiresAt
) {
}
