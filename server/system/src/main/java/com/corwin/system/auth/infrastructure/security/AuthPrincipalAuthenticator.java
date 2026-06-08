package com.corwin.system.auth.infrastructure.security;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.error.BizException;
import com.corwin.framework.json.Json;
import com.corwin.framework.util.HighDate;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.framework.web.auth.TokenPayload;
import com.corwin.system.auth.application.error.AuthError;
import com.corwin.system.auth.application.service.AuthConfigService;
import com.corwin.system.auth.domain.model.LoginSession;
import com.corwin.system.auth.domain.model.SessionStatus;
import com.corwin.system.auth.domain.repo.LoginSessionRepository;
import com.corwin.system.resource.application.service.PermissionService;
import com.corwin.system.user.domain.model.DefaultUser;
import com.corwin.system.user.domain.model.User;
import com.corwin.system.user.domain.model.UserStatus;
import com.corwin.system.user.domain.repo.UserRepository;
import com.corwin.system.webuser.application.service.WebUserJwtTokenService;
import com.corwin.system.webuser.application.service.WebUserRestrictionService;
import com.corwin.system.webuser.domain.model.WebUser;
import com.corwin.system.webuser.domain.repo.WebUserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Corwin 2026/4/19
 */
@Component
public class AuthPrincipalAuthenticator {

    private final AuthSessionCacheService authSessionCacheService;
    private final LoginSessionRepository loginSessionRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final AuthConfigService authConfigService;
    private final OpaqueTokenService opaqueTokenService;
    private final WebUserRepository webUserRepository;
    private final WebUserRestrictionService webUserRestrictionService;
    private final WebUserJwtTokenService webUserJwtTokenService;

    public AuthPrincipalAuthenticator(AuthSessionCacheService authSessionCacheService,
            LoginSessionRepository loginSessionRepository,
            UserRepository userRepository, PermissionService permissionService, AuthConfigService authConfigService,
            OpaqueTokenService opaqueTokenService, WebUserRepository webUserRepository,
            WebUserRestrictionService webUserRestrictionService, WebUserJwtTokenService webUserJwtTokenService) {
        this.authSessionCacheService = authSessionCacheService;
        this.loginSessionRepository = loginSessionRepository;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
        this.authConfigService = authConfigService;
        this.opaqueTokenService = opaqueTokenService;
        this.webUserRepository = webUserRepository;
        this.webUserRestrictionService = webUserRestrictionService;
        this.webUserJwtTokenService = webUserJwtTokenService;
    }

    public AuthPrincipal authenticateInternalToken(String rawToken) {
        String tokenHash = opaqueTokenService.hash(rawToken);
        Optional<AuthPrincipal> cached = authSessionCacheService.get(tokenHash);
        if (cached.isPresent()) {
            return cached.get();
        }

        LoginSession session = loginSessionRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BizException(AuthError.INVALID_TOKEN));
        Instant now = HighDate.mockInstant();
        if (session.expired(now)) {
            session.markExpired(systemOperator());
            loginSessionRepository.save(session);
            authSessionCacheService.delete(tokenHash);
            throw new BizException(AuthError.TOKEN_EXPIRED);
        }
        if (session.getSessionStatus() == SessionStatus.REVOKED || session.getSessionStatus() == SessionStatus.KICKED_OUT) {
            authSessionCacheService.delete(tokenHash);
            throw new BizException(AuthError.TOKEN_REVOKED);
        }
        if (!session.active()) {
            authSessionCacheService.delete(tokenHash);
            throw new BizException(AuthError.INVALID_TOKEN);
        }

        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new BizException(AuthError.INVALID_TOKEN));
        if (user.getUserStatus() != UserStatus.ENABLED) {
            throw new BizException(AuthError.USER_DISABLED);
        }

        AuthPrincipal principal = new AuthPrincipal(user.getId(), user.getUsername(), user.getUserType(),
                DefaultUser.isAdmin(user.getId()), readPermissionSnapshot(session));
        authSessionCacheService.set(tokenHash, principal, cacheTtl(session, now));
        refreshLastAccessIfNecessary(session, now, user.getUsername());
        return principal;
    }

    public AuthPrincipal authenticateExternalToken(String rawToken) {
        WebUserJwtTokenService.WebUserJwtPayload payload = webUserJwtTokenService.parse(rawToken);
        if (payload.userId() == null || payload.userType() != UserType.EXTERNAL) {
            throw new BizException(AuthError.INVALID_TOKEN);
        }
        WebUser user = webUserRepository.findById(payload.userId())
                .orElseThrow(() -> new BizException(AuthError.INVALID_TOKEN));
        if (!user.canLogin()) {
            throw new BizException(AuthError.USER_DISABLED);
        }
        if (payload.tokenVersion() == null || !payload.tokenVersion().equals(user.getTokenVersion())) {
            throw new BizException(AuthError.TOKEN_REVOKED);
        }
        if (payload.issuedAt() == null || (user.getTokenNotBefore() != null && payload.issuedAt().isBefore(user.getTokenNotBefore()))) {
            throw new BizException(AuthError.TOKEN_REVOKED);
        }
        if (webUserRestrictionService.hasLoginRestriction(user.getId())) {
            throw new BizException(AuthError.FORBIDDEN);
        }
        Set<String> permissionCodes = permissionService.permissionCodesForUser(user.getId(), UserType.EXTERNAL);
        return new AuthPrincipal(user.getId(), payload.account(), UserType.EXTERNAL, false, permissionCodes);
    }

    public AuthPrincipal authenticateSseTicket(TokenPayload payload) {
        if (payload == null || payload.userId() == null || payload.userAccount() == null || payload.userAccount()
                .isBlank()) {
            throw new BizException(AuthError.INVALID_TOKEN);
        }
        UserType userType = payload.userType();
        Set<String> permissionCodes = userType == null || userType == UserType.GUEST
                ? Set.of()
                : permissionService.permissionCodesForUser(payload.userId(), userType);
        return new AuthPrincipal(payload.userId(), payload.userAccount(), userType,
                DefaultUser.isAdmin(payload.userId()), permissionCodes);
    }

    public void cacheSession(String tokenHash, AuthPrincipal principal, Duration ttl) {
        authSessionCacheService.set(tokenHash, principal, ttl);
    }

    public void evictSession(String tokenHash) {
        authSessionCacheService.delete(tokenHash);
    }

    private Duration cacheTtl(LoginSession session, Instant now) {
        long seconds = Math.max(1L, Duration.between(now, session.getExpiresAt()).getSeconds());
        long maxCacheSeconds = Math.max(1L, authConfigService.sessionCacheTtl().getSeconds());
        return Duration.ofSeconds(Math.min(seconds, maxCacheSeconds));
    }

    private void refreshLastAccessIfNecessary(LoginSession session, Instant now, String operator) {
        Duration interval = authConfigService.lastAccessRefreshInterval();
        if (interval.isZero() || interval.isNegative()) {
            return;
        }
        if (Duration.between(session.getLastAccessAt(), now).compareTo(interval) < 0) {
            return;
        }
        session.refreshLastAccess(now, operator);
        loginSessionRepository.save(session);
    }

    private Set<String> readPermissionSnapshot(LoginSession session) {
        String raw = session.getPermissionSnapshotJson();
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        List<String> values = Json.parse(raw, new TypeReference<>() {
        });
        return new LinkedHashSet<>(values);
    }

    private String systemOperator() {
        return DefaultUser.SYSTEM.account();
    }
}
