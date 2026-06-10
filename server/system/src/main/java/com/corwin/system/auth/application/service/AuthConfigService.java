package com.corwin.system.auth.application.service;

import java.time.Duration;

/**
 * @author Corwin 2026/4/19
 */
public interface AuthConfigService {

    Duration internalSessionTtl();

    boolean internalSingleLoginEnabled();

    Duration sessionCacheTtl();

    Duration lastAccessRefreshInterval();

    Duration externalAccessTokenTtl();

    Duration externalRefreshTokenTtl();

    Duration externalAccessTokenRefreshSkew();

    String externalJwtSecret();

    String externalJwtIssuer();
}
