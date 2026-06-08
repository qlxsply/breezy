package com.corwin.system.webuser.application.view;

import com.corwin.framework.constant.UserType;

/**
 * @author Corwin 2026/5/11
 */
public record WebUserAuthView(
        Long id,
        String account,
        UserType userType,
        boolean mustChangePassword
) {
}
