package com.corwin.system.user.interfaces.web.req;

/**
 * @author Corwin 2026/4/19
 */
public record ChangeMyPasswordReq(
        String oldPassword,
        String newPassword
) {
}
