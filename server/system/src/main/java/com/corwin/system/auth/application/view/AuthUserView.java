package com.corwin.system.auth.application.view;

import com.corwin.framework.constant.UserType;

/**
 * View object representing basic authenticated user information.
 *
 * @author Corwin 2026/1/22
 */
public record AuthUserView(
    Long id, String account, UserType userType, boolean mustChangePassword) {}
