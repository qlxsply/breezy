package com.corwin.system.auth.application.service;

import com.corwin.framework.config.runtime.Configs;
import com.corwin.system.auth.config.SystemAuthConfigSpecs;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * @author Corwin 2026/4/19
 */
@Service
public class DefaultAuthConfigService implements AuthConfigService {

    private static final String JWT_SECRET_PROPERTY = "breezy.auth.jwt-secret";

    private final String jwtSecret;

    public DefaultAuthConfigService(Environment environment) {
        String configuredSecret = environment.getRequiredProperty(JWT_SECRET_PROPERTY).trim();
        if (configuredSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(JWT_SECRET_PROPERTY + " must contain at least 32 UTF-8 bytes");
        }
        jwtSecret = configuredSecret;
    }

    @Override
    public Duration internalSessionTtl() {
        return Duration.ofSeconds(config().internalSessionTtlSeconds());
    }

    @Override
    public boolean internalSingleLoginEnabled() {
        return config().internalSingleLoginEnabled();
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
    public Duration externalAccessTokenTtl() {
        return Duration.ofSeconds(config().externalAccessTokenTtlSeconds());
    }

    @Override
    public Duration externalRefreshTokenTtl() {
        return Duration.ofSeconds(config().externalRefreshTokenTtlSeconds());
    }

    @Override
    public Duration externalAccessTokenRefreshSkew() {
        return Duration.ofSeconds(config().externalAccessTokenRefreshSkewSeconds());
    }

    @Override
    public String externalJwtSecret() {
        return jwtSecret;
    }

    @Override
    public String externalJwtIssuer() {
        return config().externalJwtIssuer();
    }

    private SystemAuthConfigSpecs.AuthenticationConfig config() {
        return Configs.get(SystemAuthConfigSpecs.AUTHENTICATION);
    }
}
