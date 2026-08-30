package com.corwin.system.user.interfaces.web.res;

/**
 * Response DTO for a user configuration entry.
 *
 * @author Corwin 2026/2/2
 */
public record UserConfigsRes(String code, String description, String valueType, String value) {}
