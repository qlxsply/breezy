package com.corwin.system.methodstat.interfaces.web.res;

/**
 * Response DTO for per-method switch state, used in API responses.
 *
 * @author Corwin 2026/3/25
 */
public record MethodStatMethodSwitchRes(String key, boolean enabled) {}
