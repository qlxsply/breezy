package com.corwin.system.auth.application.service;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.error.BizException;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.system.auth.application.error.AuthError;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Service that performs runtime authorization checks against the current
 * security context, enforcing user type and permission requirements.
 *
 * @author Corwin 2026/4/19
 */
@Service
public class RequestAuthorizationService {

    private final DefaultSecurityContextService securityContextService;

    public RequestAuthorizationService(DefaultSecurityContextService securityContextService) {
        this.securityContextService = securityContextService;
    }

    /**
     * Checks that the current user is authenticated and optionally matches the given user type.
     */
    public void checkAuthenticated(UserType userType) {
        AuthPrincipal principal = securityContextService.current();
        checkUserType(principal, userType);
    }

    /**
     * Checks that the current user is authenticated, matches the given user type,
     * and possesses the required permission codes.
     */
    public void checkAuthorized(UserType userType, String[] permissions, boolean anyPermission) {
        AuthPrincipal principal = securityContextService.current();
        checkUserType(principal, userType);
        if (principal.admin() && principal.userType() == UserType.ADMIN) {
            return;
        }
        if (permissions == null || permissions.length == 0) {
            return;
        }
        Set<String> granted = principal.permissionCodes();
        boolean matched = anyPermission
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
}
