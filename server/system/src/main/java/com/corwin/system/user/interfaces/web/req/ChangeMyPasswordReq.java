package com.corwin.system.user.interfaces.web.req;

/**
 * Request object for an end-user changing their own password.
 *
 * @author Corwin 2026/4/19
 */
public record ChangeMyPasswordReq(String oldPassword, String newPassword) {}
