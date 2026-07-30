package com.corwin.system.auth.interfaces.web.res;

import com.corwin.framework.constant.UserType;
import com.corwin.system.user.interfaces.web.res.UserConfigsRes;

import java.util.List;

/**
 * Response DTO for authenticated user information.
 *
 * @author Corwin 2026/1/22
 */
public record AuthUserRes(
        String id,
        String account,
        UserType userType,
        boolean mustChangePassword,
        List<UserConfigsRes> configs
) {
}
