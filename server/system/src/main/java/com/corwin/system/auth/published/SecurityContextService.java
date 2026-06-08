package com.corwin.system.auth.published;

import com.corwin.framework.web.auth.AuthPrincipal;

import java.util.Optional;

/**
 * @author Corwin 2026/4/19
 */
public interface SecurityContextService {

    AuthPrincipal current();

    Optional<AuthPrincipal> currentOptional();

    boolean isAuthenticated();

    boolean isInternalUser();

    boolean isExternalUser();

    boolean isNormalUser();

    boolean isAdmin();
}
