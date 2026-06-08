package com.corwin.system.auth.application.service;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.error.BizException;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.system.auth.application.error.AuthError;
import org.springframework.stereotype.Service;

import java.util.Arrays;
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

    public void checkAuthenticated(UserType[] userTypes) {
        AuthPrincipal principal = securityContextService.current();
        checkUserType(principal, userTypes);
    }

    public void checkAuthorized(UserType[] userTypes, String[] permissions, boolean anyPermission) {
        AuthPrincipal principal = securityContextService.current();
        checkUserType(principal, userTypes);
        if (principal.admin() && principal.userType() == UserType.INTERNAL) {
            return;
        }
        if (permissions == null || permissions.length == 0) {
            return;
        }
        Set<String> granted = principal.permissionCodes();
        boolean matched = anyPermission ? Arrays.stream(permissions).filter(code -> code != null && !code.isBlank())
                                          .map(String::trim).anyMatch(granted::contains) : Arrays.stream(permissions)
                                                                                           .filter(code -> code != null && !code.isBlank())
                                                                                           .map(String::trim)
                                                                                           .allMatch(granted::contains);
        if (!matched) {
            throw new BizException(AuthError.FORBIDDEN);
        }
    }

    private void checkUserType(AuthPrincipal principal, UserType[] userTypes) {
        if (userTypes == null || userTypes.length == 0) {
            return;
        }
        boolean matched = Arrays.stream(userTypes).anyMatch(type -> type == principal.userType());
        if (!matched) {
            throw new BizException(AuthError.FORBIDDEN);
        }
    }
}
