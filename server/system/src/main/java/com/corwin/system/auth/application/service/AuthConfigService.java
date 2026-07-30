package com.corwin.system.auth.application.service;

import java.time.Duration;

/**
 * Configuration service interface providing authentication-related settings
 * such as session TTLs, JWT secrets, and cache durations.
 *
 * @author Corwin 2026/4/19
 */
public interface AuthConfigService {

    /**
     * Returns the TTL for internal (admin) login sessions.
     */
    Duration internalSessionTtl();

    /**
     * Returns whether single login enforcement (kick previous session) is enabled for internal users.
     */
    boolean internalSingleLoginEnabled();

    /**
     * Returns the TTL for cached session data.
     */
    Duration sessionCacheTtl();

    /**
     * Returns the minimum interval between last-access timestamp updates.
     */
    Duration lastAccessRefreshInterval();

    /**
     * Returns the TTL for external (web user) access tokens.
     */
    Duration externalAccessTokenTtl();

    /**
     * Returns the TTL for external (web user) refresh tokens.
     */
    Duration externalRefreshTokenTtl();

    /**
     * Returns the refresh skew duration for access token expiry checks.
     */
    Duration externalAccessTokenRefreshSkew();

    /**
     * Returns the JWT signing secret for external user tokens.
     */
    String externalJwtSecret();

    /**
     * Returns the JWT issuer for external user tokens.
     */
    String externalJwtIssuer();
}
