package com.corwin.system.methodstat.interfaces.web.req;

/**
 * Request DTO for updating a per-method statistics switch.
 * @author Corwin 2026/3/25
 */
public record MethodStatMethodSwitchUpdateReq(
        String key,
        boolean enabled
) {
}
