package com.corwin.system.auth.application.service;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.error.BizException;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.system.auth.application.error.AuthError;
import com.corwin.system.user.domain.model.User;
import com.corwin.system.user.domain.repo.UserRepository;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Service that performs runtime authorization checks against the current security context,
 * enforcing user type and permission requirements.
 *
 * @author Corwin 2026/4/19
 */
@Service
public class RequestAuthorizationService {

  private final DefaultSecurityContextService securityContextService;
  private final UserRepository userRepository;

  public RequestAuthorizationService(
      DefaultSecurityContextService securityContextService, UserRepository userRepository) {
    this.securityContextService = securityContextService;
    this.userRepository = userRepository;
  }

  /** Checks that the current user is authenticated and optionally matches the given user type. */
  public void checkAuthenticated(
      UserType userType, boolean allowExpiredCredentials, boolean optional) {
    AuthPrincipal principal =
        optional
            ? securityContextService.currentOptional().orElse(null)
            : securityContextService.current();
    if (principal == null) {
      return;
    }
    checkUserType(principal, userType);
    checkCredentials(principal, allowExpiredCredentials);
  }

  /**
   * Checks that the current user is authenticated, matches the given user type, and possesses the
   * required permission codes.
   */
  public void checkAuthorized(UserType userType, String[] permissions, boolean anyPermission) {
    AuthPrincipal principal = securityContextService.current();
    checkUserType(principal, userType);
    checkCredentials(principal, false);
    if (principal.admin() && principal.userType() == UserType.ADMIN) {
      return;
    }
    if (permissions == null || permissions.length == 0) {
      return;
    }
    Set<String> granted = principal.permissionCodes();
    boolean matched =
        anyPermission
            ? java.util.Arrays.stream(permissions)
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .anyMatch(granted::contains)
            : java.util.Arrays.stream(permissions)
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .allMatch(granted::contains);
    if (!matched) {
      throw new BizException(AuthError.FORBIDDEN);
    }
  }

  private void checkUserType(AuthPrincipal principal, UserType userType) {
    if (userType == null || userType == UserType.GUEST) {
      return;
    }
    if (userType != principal.userType()) {
      throw new BizException(AuthError.FORBIDDEN);
    }
  }

  private void checkCredentials(AuthPrincipal principal, boolean allowExpiredCredentials) {
    if (allowExpiredCredentials) {
      return;
    }
    boolean credentialsExpired = principal.credentialsExpired();
    if (principal.userType() == UserType.ADMIN && principal.userId() != null) {
      credentialsExpired =
          userRepository.findById(principal.userId()).map(User::isMustChangePassword).orElse(true);
    }
    if (credentialsExpired) {
      throw new BizException(AuthError.FORBIDDEN);
    }
  }
}
