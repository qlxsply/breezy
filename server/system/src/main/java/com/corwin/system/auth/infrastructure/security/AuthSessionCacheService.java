package com.corwin.system.auth.infrastructure.security;

import com.corwin.framework.cache.CacheMode;
import com.corwin.framework.cache.CacheObjectOps;
import com.corwin.framework.cache.CacheTemplate;
import com.corwin.framework.web.auth.AuthPrincipal;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Local cache service for {@link AuthPrincipal} session data, keyed by
 * token hash, to reduce repeated database lookups on authenticated requests.
 *
 * @author Corwin 2026/5/7
 */
@Service
public class AuthSessionCacheService {

    private static final String SESSION_CACHE_KEY_PREFIX = "auth:session:";

    private final CacheObjectOps<AuthPrincipal> sessionCache;

    public AuthSessionCacheService(CacheTemplate cacheTemplate) {
        this.sessionCache = cacheTemplate.objectOps(CacheMode.LOCAL, AuthPrincipal.class);
    }

    /**
     * Retrieves a cached principal by token hash.
     *
     * @param tokenHash the SHA-256 hash of the token
     * @return an {@link Optional} containing the cached principal, or empty
     */
    public Optional<AuthPrincipal> get(String tokenHash) {
        return sessionCache.get(cacheKey(tokenHash));
    }

    /**
     * Caches a principal for the given token hash with a TTL.
     *
     * @param tokenHash the SHA-256 hash of the token
     * @param principal the principal to cache
     * @param ttl       the time-to-live duration
     */
    public void set(String tokenHash, AuthPrincipal principal, Duration ttl) {
        sessionCache.set(cacheKey(tokenHash), principal, ttl);
    }

    /**
     * Removes a cached principal entry.
     *
     * @param tokenHash the SHA-256 hash of the token
     */
    public void delete(String tokenHash) {
        sessionCache.delete(cacheKey(tokenHash));
    }

    private String cacheKey(String tokenHash) {
        return SESSION_CACHE_KEY_PREFIX + tokenHash;
    }
}
