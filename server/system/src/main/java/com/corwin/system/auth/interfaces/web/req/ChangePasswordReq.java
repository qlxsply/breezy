package com.corwin.system.auth.interfaces.web.req;

/**
 * @author Corwin 2026/1/23
 */
public record ChangePasswordReq(
        String oldPassword,
        String newPassword
) {
}
