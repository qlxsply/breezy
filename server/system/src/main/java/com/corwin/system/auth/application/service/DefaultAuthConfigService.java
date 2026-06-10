package com.corwin.system.auth.application.service;

import com.corwin.framework.config.ConfigStore;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * @author Corwin 2026/4/19
 */
@Service
public class DefaultAuthConfigService implements AuthConfigService {

    private static final String INTERNAL_SESSION_TTL_SECONDS = "auth.internal.session-ttl-seconds";
    private static final String INTERNAL_SINGLE_LOGIN_ENABLED = "auth.internal.single-login-enabled";
    private static final String SESSION_CACHE_TTL_SECONDS = "auth.cache.session-ttl-seconds";
    private static final String LAST_ACCESS_REFRESH_INTERVAL_SECONDS = "auth.internal.update-last-access-interval-seconds";
    private static final String EXTERNAL_ACCESS_TOKEN_TTL_SECONDS = "auth.external.access-token-ttl-seconds";
    private static final String EXTERNAL_REFRESH_TOKEN_TTL_SECONDS = "auth.external.refresh-token-ttl-seconds";
    private static final String EXTERNAL_ACCESS_TOKEN_REFRESH_SKEW_SECONDS = "auth.external.access-token-refresh-skew-seconds";
    private static final String EXTERNAL_JWT_SECRET = "auth.external.jwt-secret";
    private static final String EXTERNAL_JWT_ISSUER = "auth.external.jwt-issuer";

    private final ConfigStore configStore;

    public DefaultAuthConfigService(ConfigStore configStore) {
        this.configStore = configStore;
    }

    @Override
    public Duration internalSessionTtl() {
        return Duration.ofSeconds(longValue(INTERNAL_SESSION_TTL_SECONDS, 8 * 60 * 60));
    }

    @Override
    public boolean internalSingleLoginEnabled() {
        return booleanValue(INTERNAL_SINGLE_LOGIN_ENABLED, false);
    }

    @Override
    public Duration sessionCacheTtl() {
        return Duration.ofSeconds(longValue(SESSION_CACHE_TTL_SECONDS, 300));
    }

    @Override
    public Duration lastAccessRefreshInterval() {
        return Duration.ofSeconds(longValue(LAST_ACCESS_REFRESH_INTERVAL_SECONDS, 60));
    }

    @Override
    public Duration externalAccessTokenTtl() {
        return Duration.ofSeconds(longValue(EXTERNAL_ACCESS_TOKEN_TTL_SECONDS, 30 * 60));
    }

    @Override
    public Duration externalRefreshTokenTtl() {
        return Duration.ofSeconds(longValue(EXTERNAL_REFRESH_TOKEN_TTL_SECONDS, 7 * 24 * 60 * 60));
    }

    @Override
    public Duration externalAccessTokenRefreshSkew() {
        return Duration.ofSeconds(longValue(EXTERNAL_ACCESS_TOKEN_REFRESH_SKEW_SECONDS, 60));
    }

    @Override
    public String externalJwtSecret() {
        return stringValue(EXTERNAL_JWT_SECRET, "breezy-external-user-jwt-secret-please-change");
    }

    @Override
    public String externalJwtIssuer() {
        return stringValue(EXTERNAL_JWT_ISSUER, "breezy");
    }

    private long longValue(String code, long defaultValue) {
        return configStore.findByCode(code).map(stored -> stored.value().trim()).filter(raw -> !raw.isBlank())
                .map(raw -> {
                    try {
                        return Long.parseLong(raw);
                    } catch (NumberFormatException ex) {
                        return defaultValue;
                    }
                }).orElse(defaultValue);
    }

    private boolean booleanValue(String code, boolean defaultValue) {
        return configStore.findByCode(code).map(stored -> stored.value().trim()).filter(raw -> !raw.isBlank())
                .map(Boolean::parseBoolean).orElse(defaultValue);
    }

    private String stringValue(String code, String defaultValue) {
        return configStore.findByCode(code).map(stored -> stored.value().trim()).filter(raw -> !raw.isBlank())
                .orElse(defaultValue);
    }
}
