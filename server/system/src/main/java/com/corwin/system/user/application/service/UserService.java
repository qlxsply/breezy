package com.corwin.system.user.application.service;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.system.auth.application.command.ChangePasswordCommand;
import com.corwin.system.auth.application.error.AuthError;
import com.corwin.system.auth.application.service.PasswordPolicyService;
import com.corwin.system.auth.published.SecurityContextService;
import com.corwin.system.user.application.command.RegisterUserCommand;
import com.corwin.system.user.application.command.UpdateMyProfileCommand;
import com.corwin.system.user.application.view.UserProfileView;
import com.corwin.system.userfeature.application.service.UserFeatureAccessService;
import com.corwin.system.webuser.application.service.WebUserAuthService;
import com.corwin.system.webuser.application.service.WebUserIdentitySupport;
import com.corwin.system.webuser.application.service.WebUserLifecycleService;
import com.corwin.system.webuser.domain.model.WebUser;
import com.corwin.system.webuser.domain.model.WebUserCredential;
import com.corwin.system.webuser.domain.model.WebUserCredentialType;
import com.corwin.system.webuser.domain.model.WebUserCurrentIdentity;
import com.corwin.system.webuser.domain.model.WebUserIdentity;
import com.corwin.system.webuser.domain.model.WebUserIdentityType;
import com.corwin.system.webuser.domain.model.WebUserLifecycleEventType;
import com.corwin.system.webuser.domain.model.WebUserRegisterChannel;
import com.corwin.system.webuser.domain.model.WebUserRegisterMethod;
import com.corwin.system.webuser.domain.repo.WebUserCredentialRepository;
import com.corwin.system.webuser.domain.repo.WebUserCurrentIdentityRepository;
import com.corwin.system.webuser.domain.repo.WebUserIdentityRepository;
import com.corwin.system.webuser.domain.repo.WebUserRepository;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Application service for end-user (non-admin) operations including
 * self-registration, profile retrieval and update, password change, and logout.
 *
 * @author Corwin 2026/4/19
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final WebUserRepository webUserRepository;
    private final WebUserIdentityRepository webUserIdentityRepository;
    private final WebUserCredentialRepository webUserCredentialRepository;
    private final WebUserCurrentIdentityRepository webUserCurrentIdentityRepository;
    private final PasswordPolicyService passwordPolicyService;
    private final SecurityContextService securityContextService;
    private final UserFeatureAccessService userFeatureAccessService;
    private final WebUserAuthService webUserAuthService;
    private final WebUserLifecycleService webUserLifecycleService;
    private final WebUserIdentitySupport webUserIdentitySupport;

    /**
     * Registers a new end-user with username/password credentials, creates identity and credential records,
     * assigns default feature packages, and records the registration lifecycle event.
     *
     * @param cmd the registration command containing username, nickname, and password
     * @return the registered user's profile view
     */
    @Transactional
    public UserProfileView register(RegisterUserCommand cmd) {
        BizAssert.notNull(cmd, BaseError.INVALID_PARAMETER);
        String username = normalizeUsername(cmd.username());
        String nickname = normalizeNickname(cmd.nickname(), username);
        passwordPolicyService.validate(cmd.password());

        String identityHash = webUserIdentitySupport.hash(WebUserIdentityType.USERNAME, username);
        BizAssert.state(webUserCurrentIdentityRepository.findByIdentityTypeAndIdentityHash(WebUserIdentityType.USERNAME,
                identityHash).isEmpty(), BaseError.CONFLICT);

        WebUser user = webUserRepository.save(new WebUser(nickname, nickname, WebUserRegisterMethod.USERNAME_PASSWORD,
                WebUserRegisterChannel.WEB, "self-register", null, null, username));
        WebUserIdentity identity = webUserIdentityRepository.save(new WebUserIdentity(user.getId(),
                WebUserIdentityType.USERNAME, username,
                webUserIdentitySupport.normalizeForType(WebUserIdentityType.USERNAME, username), identityHash, null,
                null, true, true, username));
        webUserCredentialRepository.save(new WebUserCredential(user.getId(), identity.getId(),
                WebUserCredentialType.PASSWORD, BCrypt.hashpw(cmd.password(), BCrypt.gensalt()), "bcrypt", username));
        webUserCurrentIdentityRepository.save(new WebUserCurrentIdentity(WebUserIdentityType.USERNAME, identityHash,
                null, null, user.getId(), identity.getId()));
        user.setPrimaryIdentityId(identity.getId(), username);
        user = webUserRepository.save(user);
        userFeatureAccessService.assignDefaultPackagesToUser(user.getId());
        webUserLifecycleService.record(user.getId(), WebUserLifecycleEventType.REGISTERED,
                Map.of("registerMethod", WebUserRegisterMethod.USERNAME_PASSWORD.name()));
        return toView(user, identity.getIdentityValue());
    }

    /**
     * Returns the profile of the currently authenticated end-user.
     *
     * @return the current user's profile view
     */
    public UserProfileView currentProfile() {
        WebUser user = currentUser();
        return toView(user, resolveAccount(user));
    }

    /**
     * Updates the profile (nickname) of the currently authenticated end-user.
     *
     * @param cmd the update command containing the new nickname
     * @return the updated profile view
     */
    @Transactional
    public UserProfileView updateMyProfile(UpdateMyProfileCommand cmd) {
        BizAssert.notNull(cmd, BaseError.INVALID_PARAMETER);
        WebUser user = currentUser();
        AuthPrincipal principal = currentPrincipal();
        String nickname = normalizeNickname(cmd.nickname(), resolveAccount(user));
        user.updateProfile(nickname, nickname, principal.username());
        user = webUserRepository.save(user);
        webUserLifecycleService.record(user.getId(), WebUserLifecycleEventType.PROFILE_UPDATED, Map.of());
        return toView(user, resolveAccount(user));
    }

    /**
     * Changes the password for the currently authenticated end-user.
     *
     * @param cmd the change password command containing old and new passwords
     * @return true if the password was changed successfully
     */
    public boolean changeMyPassword(ChangePasswordCommand cmd) {
        return webUserAuthService.changePassword(cmd);
    }

    /**
     * Logs out the currently authenticated end-user.
     *
     * @return true if the logout was successful
     */
    public boolean logout() {
        return webUserAuthService.logout();
    }

    private WebUser currentUser() {
        AuthPrincipal principal = currentPrincipal();
        return webUserRepository.findById(principal.userId())
                .orElseThrow(() -> new BizException(AuthError.INVALID_TOKEN));
    }

    private AuthPrincipal currentPrincipal() {
        AuthPrincipal principal = securityContextService.current();
        BizAssert.state(principal.userType() == UserType.USER, AuthError.FORBIDDEN);
        return principal;
    }

    private String normalizeUsername(String username) {
        BizAssert.notBlank(username, BaseError.MISSING_PARAMETER);
        return username.trim();
    }

    private String normalizeNickname(String nickname, String fallback) {
        if (nickname == null || nickname.isBlank()) {
            return fallback;
        }
        return nickname.trim();
    }

    private String resolveAccount(WebUser user) {
        Long primaryIdentityId = user.getPrimaryIdentityId();
        if (primaryIdentityId != null) {
            return webUserIdentityRepository.findById(primaryIdentityId).map(WebUserIdentity::getIdentityValue)
                    .orElse(String.valueOf(user.getId()));
        }
        return webUserIdentityRepository.findFirstByUserIdAndIdentityType(user.getId(), WebUserIdentityType.USERNAME)
                .map(WebUserIdentity::getIdentityValue)
                .orElse(String.valueOf(user.getId()));
    }

    private UserProfileView toView(WebUser user, String account) {
        return new UserProfileView(user.getId(), account, user.getNickname(), UserType.USER,
                user.getStatus().name(), false, user.getCreatedAt(), user.getUpdatedAt());
    }
}
