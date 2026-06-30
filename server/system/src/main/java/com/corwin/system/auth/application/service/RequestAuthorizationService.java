package com.corwin.system.auth.application.service;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.error.BizException;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.system.auth.application.error.AuthError;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * @author Corwin 2026/4/19
 */
@Service
public class RequestAuthorizationService {

    private final DefaultSecurityContextService securityContextService;

    public RequestAuthorizationService(DefaultSecurityContextService securityContextService) {
        this.securityContextService = securityContextService;
    }

    public void checkAuthenticated(UserType userType) {
        AuthPrincipal principal = securityContextService.current();
        checkUserType(principal, userType);
    }

    public void checkAuthorized(UserType userType, String[] permissions, boolean anyPermission) {
        AuthPrincipal principal = securityContextService.current();
        checkUserType(principal, userType);
        if (principal.admin() && principal.userType() == UserType.INTERNAL) {
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
