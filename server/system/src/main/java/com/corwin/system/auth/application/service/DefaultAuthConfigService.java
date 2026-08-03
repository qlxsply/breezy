package com.corwin.system.auth.application.service;

import com.corwin.framework.config.runtime.Configs;
import com.corwin.system.auth.config.SystemAuthConfigSpecs;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * @author Corwin 2026/4/19
 */
@Service
public class DefaultAuthConfigService implements AuthConfigService {

    @Override
    public Duration adminSessionTtl() {
        return Duration.ofSeconds(config().adminSessionTtlSeconds());
    }

    @Override
    public boolean adminSingleLoginEnabled() {
        return config().adminSingleLoginEnabled();
    }

    @Override
    public Duration sessionCacheTtl() {
        return Duration.ofSeconds(config().sessionCacheTtlSeconds());
    }

    @Override
    public Duration lastAccessRefreshInterval() {
        return Duration.ofSeconds(config().lastAccessRefreshIntervalSeconds());
    }

    @Override
    public Duration userAccessTokenTtl() {
        return Duration.ofSeconds(config().userAccessTokenTtlSeconds());
    }

    @Override
    public Duration userRefreshTokenTtl() {
        return Duration.ofSeconds(config().userRefreshTokenTtlSeconds());
    }

    @Override
    public Duration userAccessTokenRefreshSkew() {
        return Duration.ofSeconds(config().userAccessTokenRefreshSkewSeconds());
    }

    @Override
    public String userJwtSecret() {
        return config().userJwtSecret();
    }

    @Override
    public String userJwtIssuer() {
        return config().userJwtIssuer();
    }

    private SystemAuthConfigSpecs.AuthenticationConfig config() {
        return Configs.get(SystemAuthConfigSpecs.AUTHENTICATION);
    }
}
