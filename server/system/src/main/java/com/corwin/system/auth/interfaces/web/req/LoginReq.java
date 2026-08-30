package com.corwin.system.auth.interfaces.web.req;

/**
 * Request DTO for user login with account and password.
 *
 * @author Corwin 2026/1/22
 */
public record LoginReq(String account, String password) {}
