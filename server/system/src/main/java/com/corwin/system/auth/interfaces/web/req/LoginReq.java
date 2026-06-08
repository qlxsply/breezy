package com.corwin.system.auth.interfaces.web.req;

/**
 * @author Corwin 2026/1/22
 */
public record LoginReq(
        String account,
        String password
) {
}
