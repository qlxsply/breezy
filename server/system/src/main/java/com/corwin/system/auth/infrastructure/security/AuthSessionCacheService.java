package com.corwin.system.auth.infrastructure.security;

import com.corwin.framework.cache.CacheMode;
import com.corwin.framework.cache.CacheObjectOps;
import com.corwin.framework.cache.CacheTemplate;
import com.corwin.framework.web.auth.AuthPrincipal;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * @author Corwin 2026/5/7
 */
@Service
public class AuthSessionCacheService {

    private static final String SESSION_CACHE_KEY_PREFIX = "auth:session:";

    private final CacheObjectOps<AuthPrincipal> sessionCache;

    public AuthSessionCacheService(CacheTemplate cacheTemplate) {
        this.sessionCache = cacheTemplate.objectOps(CacheMode.LOCAL, AuthPrincipal.class);
    }

    public Optional<AuthPrincipal> get(String tokenHash) {
        return sessionCache.get(cacheKey(tokenHash));
    }

    public void set(String tokenHash, AuthPrincipal principal, Duration ttl) {
        sessionCache.set(cacheKey(tokenHash), principal, ttl);
    }

    public void delete(String tokenHash) {
        sessionCache.delete(cacheKey(tokenHash));
    }

    private String cacheKey(String tokenHash) {
        return SESSION_CACHE_KEY_PREFIX + tokenHash;
    }
}
