package com.corwin.framework.config.definition;

/**
 * @author Corwin 2026/7/30
 */
public record ConfigViolation(
        String path,
        String code,
        String message
) {

    public ConfigViolation {
        path = path == null ? "" : path;
        requireText(code, "code");
        requireText(message, "message");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
