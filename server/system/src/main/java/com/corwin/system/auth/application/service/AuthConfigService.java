package com.corwin.system.auth.application.service;

import java.time.Duration;

/**
 * Configuration service interface providing authentication-related settings such as session TTLs,
 * JWT secrets, and cache durations.
 *
 * @author Corwin 2026/4/19
 */
public interface AuthConfigService {

  /** Returns the TTL for admin login sessions. */
  Duration adminSessionTtl();

  /**
   * Returns whether single login enforcement (kick previous session) is enabled for admin account.
   */
  boolean adminSingleLoginEnabled();

  /** Returns the TTL for cached session data. */
  Duration sessionCacheTtl();

  /** Returns the minimum interval between last-access timestamp updates. */
  Duration lastAccessRefreshInterval();

  /** Returns the TTL for user access tokens. */
  Duration userAccessTokenTtl();

  /** Returns the TTL for user refresh tokens. */
  Duration userRefreshTokenTtl();

  /** Returns the refresh skew duration for access token expiry checks. */
  Duration userAccessTokenRefreshSkew();

  /** Returns the JWT signing secret for user tokens. */
  String userJwtSecret();

  /** Returns the JWT issuer for user tokens. */
  String userJwtIssuer();
}
