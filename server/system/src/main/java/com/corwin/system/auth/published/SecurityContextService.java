package com.corwin.system.auth.published;

import com.corwin.framework.web.auth.AuthPrincipal;
import java.util.Optional;

/**
 * Service interface for accessing the current security context and authentication state of the
 * currently authenticated principal.
 *
 * @author Corwin 2026/4/19
 */
public interface SecurityContextService {

  /**
   * Returns the currently authenticated principal, or throws if unauthenticated.
   *
   * @return the current {@link AuthPrincipal}
   */
  AuthPrincipal current();

  /**
   * Returns an {@link Optional} containing the current principal if authenticated.
   *
   * @return optional current {@link AuthPrincipal}
   */
  Optional<AuthPrincipal> currentOptional();

  /**
   * Checks whether the current request is authenticated.
   *
   * @return true if authenticated
   */
  boolean isAuthenticated();

  /**
   * Checks whether the current user is an internal (admin) user.
   *
   * @return true if internal user
   */
  boolean isInternalUser();

  /**
   * Checks whether the current user is an external (regular) user.
   *
   * @return true if external user
   */
  boolean isExternalUser();

  /**
   * Checks whether the current user is a normal (non-admin) user.
   *
   * @return true if normal user
   */
  boolean isNormalUser();

  /**
   * Checks whether the current user has admin privileges.
   *
   * @return true if admin
   */
  boolean isAdmin();
}
