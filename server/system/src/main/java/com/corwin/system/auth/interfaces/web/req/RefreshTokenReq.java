package com.corwin.system.auth.interfaces.web.req;

/**
 * Request DTO for refreshing an access token using a refresh token.
 *
 * @author Corwin 2026/6/7
 */
public record RefreshTokenReq(String refreshToken) {}
