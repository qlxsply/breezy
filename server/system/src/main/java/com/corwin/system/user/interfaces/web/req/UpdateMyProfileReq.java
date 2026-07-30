package com.corwin.system.user.interfaces.web.req;

/**
 * Request object for an end-user updating their own profile (nickname).
 *
 * @author Corwin 2026/4/19
 */
public record UpdateMyProfileReq(
        String nickname
) {
}
