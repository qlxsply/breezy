package com.corwin.system.auth.application.service;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.framework.json.Json;
import com.corwin.framework.util.HighDate;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.system.auth.application.command.ChangePasswordCommand;
import com.corwin.system.auth.application.command.LoginCommand;
import com.corwin.system.auth.application.error.AuthError;
import com.corwin.system.auth.application.view.AuthUserView;
import com.corwin.system.auth.application.view.LoginView;
import com.corwin.system.auth.domain.model.LoginSession;
import com.corwin.system.auth.domain.model.SessionStatus;
import com.corwin.system.auth.domain.repo.LoginSessionRepository;
import com.corwin.system.auth.infrastructure.security.AuthPrincipalAuthenticator;
import com.corwin.system.auth.infrastructure.security.OpaqueTokenService;
import com.corwin.system.auth.published.SecurityContextService;
import com.corwin.system.resource.application.service.PermissionService;
import com.corwin.system.user.domain.model.DefaultUser;
import com.corwin.system.user.domain.model.User;
import com.corwin.system.user.domain.model.UserStatus;
import com.corwin.system.user.domain.repo.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Application service for internal (admin) user authentication: login,
 * logout, password change, and session management.
 *
 * @author Corwin 2026/1/22
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordPolicyService passwordPolicyService;
    private final LoginLogService loginLogService;
    private final LoginSessionRepository loginSessionRepository;
    private final PermissionService permissionService;
    private final AuthConfigService authConfigService;
    private final OpaqueTokenService opaqueTokenService;
    private final AuthPrincipalAuthenticator authPrincipalAuthenticator;
    private final SecurityContextService securityContextService;

    /**
     * Authenticates a user with the given credentials and creates a login session.
     *
     * @param cmd the login command containing account and password
     * @return the login view with token and user info
     */
    public LoginView login(LoginCommand cmd) {
        BizAssert.notNull(cmd, AuthError.BAD_CREDENTIALS);
        String account = cmd.account();
        try {
            BizAssert.notBlank(cmd.account(), AuthError.BAD_CREDENTIALS);
            BizAssert.notBlank(cmd.password(), AuthError.BAD_CREDENTIALS);

            account = cmd.account().trim();
            User user = userRepository.findByUsername(account)
                    .orElseThrow(() -> new BizException(AuthError.BAD_CREDENTIALS));
            BizAssert.state(!DefaultUser.isSystemUser(user.getId()), AuthError.FORBIDDEN);
            BizAssert.state(user.getUserType() == UserType.ADMIN, AuthError.FORBIDDEN);
            BizAssert.state(user.getUserStatus() == UserStatus.ENABLED, AuthError.USER_DISABLED);
            if (!BCrypt.checkpw(cmd.password(), user.getPasswordHash())) {
                BizAssert.fail(AuthError.BAD_CREDENTIALS);
            }

            if (authConfigService.adminSingleLoginEnabled()) {
                kickOutActiveSessions(user);
            }

            Set<String> permissionCodes = permissionService.permissionCodesForUser(user.getId(), UserType.ADMIN);
            String rawToken = opaqueTokenService.generateToken();
            String tokenHash = opaqueTokenService.hash(rawToken);
            Instant now = HighDate.realInstant();
            Instant expiresAt = now.plus(authConfigService.adminSessionTtl());

            LoginSession session = new LoginSession(user.getId(), newTokenId(), tokenHash,
                    com.corwin.framework.web.ctx.CtxUtil.getClientIp(), currentUserAgent(),
                    Json.toStr(permissionCodes.stream().toList()), "[]", "[]", expiresAt, user.getUsername());
            loginSessionRepository.save(session);

            AuthPrincipal principal = new AuthPrincipal(user.getId(), user.getUsername(), user.getUserType(),
                    DefaultUser.isAdmin(user.getId()), user.isMustChangePassword(), permissionCodes);
            Duration cacheTtl = Duration.between(now, expiresAt).compareTo(authConfigService.sessionCacheTtl()) < 0
                    ? Duration.between(now, expiresAt) : authConfigService.sessionCacheTtl();
            authPrincipalAuthenticator.cacheSession(tokenHash, principal, cacheTtl);

            loginLogService.logLoginSuccess(user);
            return new LoginView(rawToken, null, String.valueOf(expiresAt.toEpochMilli()), null,
                    toAuthView(user, user.getUserType()));
        } catch (RuntimeException ex) {
            loginLogService.logLoginFailure(account, ex.getMessage());
            throw ex;
        }
    }

    /**
     * Returns the currently authenticated user's info, or null if not authenticated.
     */
    public AuthUserView currentUser() {
        return securityContextService.currentOptional().flatMap(principal -> userRepository.findById(principal.userId())
                .map(user -> toAuthView(user, principal.userType()))).orElse(null);
    }

    /**
     * Changes the current user's password after validating the old password.
     *
     * @param cmd the change password command
     * @return true if successful
     */
    public boolean changePassword(ChangePasswordCommand cmd) {
        BizAssert.notBlank(cmd.oldPassword(), AuthError.BAD_CREDENTIALS);
        passwordPolicyService.validate(cmd.newPassword());

        AuthPrincipal principal = securityContextService.current();
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new BizException(AuthError.INVALID_TOKEN));
        if (!BCrypt.checkpw(cmd.oldPassword(), user.getPasswordHash())) {
            BizAssert.fail(AuthError.BAD_CREDENTIALS);
        }
        user.updatePassword(BCrypt.hashpw(cmd.newPassword(), BCrypt.gensalt()), "bcrypt", principal.username());
        user.markMustChangePassword(false, principal.username());
        userRepository.save(user);
        String tokenHash = CtxUtil.getTokenHash();
        if (tokenHash != null && !tokenHash.isBlank()) {
            authPrincipalAuthenticator.evictSession(tokenHash);
        }
        return true;
    }

    /**
     * Logs out the current user by revoking the active session.
     *
     * @return true if logout was processed
     */
    public boolean logout() {
        AuthPrincipal principal = securityContextService.currentOptional().orElse(null);
        String tokenHash = CtxUtil.getTokenHash();
        if (principal == null || tokenHash == null || tokenHash.isBlank()) {
            return true;
        }

        loginSessionRepository.findByTokenHash(tokenHash).ifPresent(session -> {
            session.revoke(principal.username());
            loginSessionRepository.save(session);
        });
        authPrincipalAuthenticator.evictSession(tokenHash);
        loginLogService.logLogoutSuccess(principal.userId(), principal.username());
        return true;
    }

    private void kickOutActiveSessions(User user) {
        List<LoginSession> activeSessions = loginSessionRepository.findByUserIdAndSessionStatus(user.getId(),
                SessionStatus.ACTIVE);
        for (LoginSession session : activeSessions) {
            session.kickOut(user.getUsername());
            loginSessionRepository.save(session);
            authPrincipalAuthenticator.evictSession(session.getTokenHash());
        }
    }

    private AuthUserView toAuthView(User user, UserType userType) {
        return new AuthUserView(user.getId(), user.getUsername(), userType, user.isMustChangePassword());
    }

    private String currentUserAgent() {
        HttpServletRequest request = currentRequest();
        return request == null ? null : request.getHeader("User-Agent");
    }

    private HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    private String newTokenId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
