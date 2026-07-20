package com.corwin.system.webuser.application.service;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.framework.util.HighDate;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.system.auth.application.command.ChangePasswordCommand;
import com.corwin.system.auth.application.command.LoginCommand;
import com.corwin.system.auth.application.error.AuthError;
import com.corwin.system.auth.application.service.PasswordPolicyService;
import com.corwin.system.auth.application.service.RefreshTokenService;
import com.corwin.system.auth.published.SecurityContextService;
import com.corwin.system.resource.application.service.PermissionService;
import com.corwin.system.webuser.application.view.WebUserAuthView;
import com.corwin.system.webuser.application.view.WebUserLoginView;
import com.corwin.system.webuser.domain.model.*;
import com.corwin.system.webuser.domain.repo.WebUserCredentialRepository;
import com.corwin.system.webuser.domain.repo.WebUserCurrentIdentityRepository;
import com.corwin.system.webuser.domain.repo.WebUserIdentityRepository;
import com.corwin.system.webuser.domain.repo.WebUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * @author Corwin 2026/5/11
 */
@Service
public class WebUserAuthService {

    private final WebUserRepository webUserRepository;
    private final WebUserIdentityRepository webUserIdentityRepository;
    private final WebUserCredentialRepository webUserCredentialRepository;
    private final WebUserCurrentIdentityRepository webUserCurrentIdentityRepository;
    private final WebUserRestrictionService webUserRestrictionService;
    private final WebUserJwtTokenService webUserJwtTokenService;
    private final PermissionService permissionService;
    private final PasswordPolicyService passwordPolicyService;
    private final SecurityContextService securityContextService;
    private final WebUserLifecycleService webUserLifecycleService;
    private final WebUserIdentitySupport webUserIdentitySupport;
    private final RefreshTokenService refreshTokenService;
    private final com.corwin.system.auth.application.service.AuthConfigService authConfigService;

    public WebUserAuthService(WebUserRepository webUserRepository, WebUserIdentityRepository webUserIdentityRepository,
                              WebUserCredentialRepository webUserCredentialRepository,
                              WebUserCurrentIdentityRepository webUserCurrentIdentityRepository,
                              WebUserRestrictionService webUserRestrictionService,
                              WebUserJwtTokenService webUserJwtTokenService, PermissionService permissionService,
                              PasswordPolicyService passwordPolicyService,
                              SecurityContextService securityContextService,
                              WebUserLifecycleService webUserLifecycleService,
                              WebUserIdentitySupport webUserIdentitySupport, RefreshTokenService refreshTokenService,
                              com.corwin.system.auth.application.service.AuthConfigService authConfigService) {
        this.webUserRepository = webUserRepository;
        this.webUserIdentityRepository = webUserIdentityRepository;
        this.webUserCredentialRepository = webUserCredentialRepository;
        this.webUserCurrentIdentityRepository = webUserCurrentIdentityRepository;
        this.webUserRestrictionService = webUserRestrictionService;
        this.webUserJwtTokenService = webUserJwtTokenService;
        this.permissionService = permissionService;
        this.passwordPolicyService = passwordPolicyService;
        this.securityContextService = securityContextService;
        this.webUserLifecycleService = webUserLifecycleService;
        this.webUserIdentitySupport = webUserIdentitySupport;
        this.refreshTokenService = refreshTokenService;
        this.authConfigService = authConfigService;
    }

    @Transactional
    public WebUserLoginView login(LoginCommand cmd) {
        BizAssert.notNull(cmd, AuthError.BAD_CREDENTIALS);
        BizAssert.notBlank(cmd.account(), AuthError.BAD_CREDENTIALS);
        BizAssert.notBlank(cmd.password(), AuthError.BAD_CREDENTIALS);

        WebUserIdentityType identityType = webUserIdentitySupport.detectType(cmd.account());
        String identityHash = webUserIdentitySupport.hash(identityType, cmd.account());
        WebUserCurrentIdentity currentIdentity = webUserCurrentIdentityRepository.findByIdentityTypeAndIdentityHash(
                identityType, identityHash).orElseThrow(() -> new BizException(AuthError.BAD_CREDENTIALS));
        WebUserIdentity identity = webUserIdentityRepository.findById(currentIdentity.getIdentityId()).orElseThrow(
                () -> new BizException(AuthError.BAD_CREDENTIALS));
        WebUser user = webUserRepository.findById(currentIdentity.getUserId())
                                        .orElseThrow(() -> new BizException(AuthError.BAD_CREDENTIALS));

        BizAssert.state(user.canLogin(), AuthError.USER_DISABLED);
        BizAssert.state(Boolean.TRUE.equals(identity.getLoginEnabled()), AuthError.FORBIDDEN);
        BizAssert.state(identity.getBindStatus() == WebUserIdentityBindStatus.ACTIVE, AuthError.FORBIDDEN);
        BizAssert.state(!webUserRestrictionService.hasLoginRestriction(user.getId()), AuthError.FORBIDDEN);

        WebUserCredential credential = webUserCredentialRepository.findFirstByUserIdAndCredentialTypeAndStatus(
                user.getId(), WebUserCredentialType.PASSWORD, WebUserCredentialStatus.ACTIVE).orElseThrow(
                () -> new BizException(AuthError.BAD_CREDENTIALS));
        if (!BCrypt.checkpw(cmd.password(), credential.getSecretHash())) {
            throw new BizException(AuthError.BAD_CREDENTIALS);
        }

        user.markLoginSuccess(CtxUtil.getClientIp(), operatorName(identity.getIdentityValue()));
        webUserRepository.save(user);

        Set<String> permissionCodes = permissionService.permissionCodesForUser(user.getId(), UserType.USER);
        AuthPrincipal principal = new AuthPrincipal(user.getId(), identity.getIdentityValue(), UserType.USER, false,
                permissionCodes);
        var accessToken = webUserJwtTokenService.issue(principal,
                user.getTokenVersion() == null ? 1L : user.getTokenVersion());
        var refreshToken = refreshTokenService.issue(user.getId(), currentUserAgent());
        webUserLifecycleService.record(user.getId(), WebUserLifecycleEventType.LOGIN_SUCCESS,
                Map.of("identityType", identityType.name()));
        return new WebUserLoginView(accessToken.token(), refreshToken.rawToken(),
                String.valueOf(toRefreshTriggerAt(accessToken.expiresAt()).toEpochMilli()),
                String.valueOf(refreshToken.expiresAt().toEpochMilli()),
                new WebUserAuthView(user.getId(), identity.getIdentityValue(), UserType.USER, false));
    }

    @Transactional
    public WebUserLoginView refresh(String rawRefreshToken) {
        var rotatedRefreshToken = refreshTokenService.rotate(rawRefreshToken, currentUserAgent());
        WebUser user = requireUser(rotatedRefreshToken.userId());
        BizAssert.state(user.canLogin(), AuthError.USER_DISABLED);
        BizAssert.state(!webUserRestrictionService.hasLoginRestriction(user.getId()), AuthError.FORBIDDEN);

        WebUserIdentity identity = webUserIdentityRepository.findByUserId(user.getId()).stream()
                                                            .filter(item -> item.getBindStatus() ==
                                                                    WebUserIdentityBindStatus.ACTIVE)
                                                            .filter(item -> Boolean.TRUE.equals(item.getLoginEnabled()))
                                                            .findFirst()
                                                            .orElseThrow(() -> new BizException(AuthError.FORBIDDEN));

        Set<String> permissionCodes = permissionService.permissionCodesForUser(user.getId(), UserType.USER);
        AuthPrincipal principal = new AuthPrincipal(user.getId(), identity.getIdentityValue(), UserType.USER, false,
                permissionCodes);
        var accessToken = webUserJwtTokenService.issue(principal,
                user.getTokenVersion() == null ? 1L : user.getTokenVersion());
        return new WebUserLoginView(accessToken.token(), rotatedRefreshToken.rawToken(),
                String.valueOf(toRefreshTriggerAt(accessToken.expiresAt()).toEpochMilli()),
                String.valueOf(rotatedRefreshToken.expiresAt().toEpochMilli()),
                new WebUserAuthView(user.getId(), identity.getIdentityValue(), UserType.USER, false));
    }

    public WebUserAuthView currentUser() {
        AuthPrincipal principal = requireExternalPrincipal();
        return new WebUserAuthView(principal.userId(), principal.username(), UserType.USER, false);
    }

    @Transactional
    public boolean changePassword(ChangePasswordCommand cmd) {
        BizAssert.notBlank(cmd.oldPassword(), AuthError.BAD_CREDENTIALS);
        BizAssert.notBlank(cmd.newPassword(), AuthError.BAD_CREDENTIALS);
        passwordPolicyService.validate(cmd.newPassword());

        AuthPrincipal principal = requireExternalPrincipal();
        WebUserCredential credential = webUserCredentialRepository.findFirstByUserIdAndCredentialTypeAndStatus(
                principal.userId(), WebUserCredentialType.PASSWORD, WebUserCredentialStatus.ACTIVE).orElseThrow(
                () -> new BizException(AuthError.INVALID_TOKEN));
        if (!BCrypt.checkpw(cmd.oldPassword(), credential.getSecretHash())) {
            throw new BizException(AuthError.BAD_CREDENTIALS);
        }
        credential.updateSecret(BCrypt.hashpw(cmd.newPassword(), BCrypt.gensalt()), "bcrypt", principal.username());
        webUserCredentialRepository.save(credential);

        WebUser user = requireUser(principal.userId());
        user.revokeTokens(principal.username());
        webUserRepository.save(user);
        refreshTokenService.revokeActiveTokens(user.getId(), "password_changed");
        webUserLifecycleService.record(user.getId(), WebUserLifecycleEventType.PASSWORD_CHANGED, Map.of());
        return true;
    }

    @Transactional
    public boolean logout() {
        AuthPrincipal principal = requireExternalPrincipal();
        WebUser user = requireUser(principal.userId());
        user.revokeTokens(principal.username());
        webUserRepository.save(user);
        refreshTokenService.revokeActiveTokens(user.getId(), "logout");
        webUserLifecycleService.record(user.getId(), WebUserLifecycleEventType.LOGOUT, Map.of());
        return true;
    }

    @Transactional
    public boolean cancelCurrentUser(String reason) {
        AuthPrincipal principal = requireExternalPrincipal();
        WebUser user = requireUser(principal.userId());
        user.cancel(reason, principal.username());
        webUserRepository.save(user);
        refreshTokenService.revokeActiveTokens(user.getId(), "cancelled");
        webUserIdentityRepository.findByUserId(user.getId())
                                 .forEach(identity -> identity.release(principal.username()));
        webUserIdentityRepository.saveAll(webUserIdentityRepository.findByUserId(user.getId()));
        webUserCredentialRepository.findFirstByUserIdAndCredentialTypeAndStatus(user.getId(),
                WebUserCredentialType.PASSWORD, WebUserCredentialStatus.ACTIVE).ifPresent(credential -> {
            credential.revoke(principal.username());
            webUserCredentialRepository.save(credential);
        });
        webUserLifecycleService.record(user.getId(), WebUserLifecycleEventType.CANCELLED,
                Map.of("reason", reason == null ? "" : reason));
        return true;
    }

    private WebUser requireUser(Long userId) {
        return webUserRepository.findById(userId).orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
    }

    private AuthPrincipal requireExternalPrincipal() {
        AuthPrincipal principal = securityContextService.current();
        BizAssert.state(principal.userType() == UserType.USER, AuthError.FORBIDDEN);
        return principal;
    }

    private String operatorName(String fallback) {
        return fallback == null || fallback.isBlank() ? "system" : fallback;
    }

    private String currentUserAgent() {
        HttpServletRequest request = currentRequest();
        return request == null ? null : request.getHeader("User-Agent");
    }

    private Instant toRefreshTriggerAt(Instant actualExpiresAt) {
        Instant now = HighDate.mockInstant();
        Instant candidate = actualExpiresAt.minus(authConfigService.externalAccessTokenRefreshSkew());
        return candidate.isAfter(now) ? candidate : now;
    }

    private HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }
}
