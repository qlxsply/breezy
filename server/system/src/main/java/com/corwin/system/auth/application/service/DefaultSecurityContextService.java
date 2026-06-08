package com.corwin.system.auth.application.service;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.error.BizException;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.system.auth.application.error.AuthError;
import com.corwin.system.auth.published.SecurityContextService;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author Corwin 2026/4/19
 */
@Service
public class DefaultSecurityContextService implements SecurityContextService {

    @Override
    public AuthPrincipal current() {
        AuthPrincipal principal = CtxUtil.getPrincipal();
        if (principal == null || !principal.authenticated()) {
            throw new BizException(AuthError.UNAUTHENTICATED);
        }
        return principal;
    }

    @Override
    public Optional<AuthPrincipal> currentOptional() {
        AuthPrincipal principal = CtxUtil.getPrincipal();
        if (principal == null || !principal.authenticated()) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    @Override
    public boolean isAuthenticated() {
        return currentOptional().isPresent();
    }

    @Override
    public boolean isInternalUser() {
        return currentOptional().map(principal -> principal.userType() == UserType.INTERNAL).orElse(false);
    }

    @Override
    public boolean isExternalUser() {
        return currentOptional().map(principal -> principal.userType() == UserType.EXTERNAL).orElse(false);
    }

    @Override
    public boolean isNormalUser() {
        return isExternalUser();
    }

    @Override
    public boolean isAdmin() {
        return currentOptional().map(AuthPrincipal::admin).orElse(false);
    }
}
