package com.corwin.system.auth.interfaces.web.req;

/**
 * Request DTO for changing a user's password.
 *
 * @author Corwin 2026/1/23
 */
public record ChangePasswordReq(String oldPassword, String newPassword) {}
