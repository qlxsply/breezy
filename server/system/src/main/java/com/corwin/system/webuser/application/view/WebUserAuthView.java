package com.corwin.system.webuser.application.view;

import com.corwin.framework.constant.UserType;

/**
 * View for authenticated external user information exposed after login.
 *
 * @param id the user ID
 * @param account the user account identifier
 * @param userType the user type classification
 * @param mustChangePassword whether the user is required to change password on next login
 * @author Corwin 2026/5/11
 */
public record WebUserAuthView(
    Long id, String account, UserType userType, boolean mustChangePassword) {}
