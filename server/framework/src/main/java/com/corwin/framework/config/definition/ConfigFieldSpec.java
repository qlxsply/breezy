package com.corwin.framework.config.definition;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * @author Corwin 2026/7/30
 */
public record ConfigFieldSpec(
        String path,
        String title,
        String description,
        ConfigFieldType type,
        boolean required,
        boolean sensitive,
        boolean readOnly,
        int order,
        String placeholder,
        BigDecimal min,
        BigDecimal max,
        Integer minLength,
        Integer maxLength,
        List<ConfigOptionItem> options
) {

    public ConfigFieldSpec {
        requireText(path, "path");
        requireText(title, "title");
        description = description == null ? "" : description;
        Objects.requireNonNull(type, "type required");
        placeholder = placeholder == null ? "" : placeholder;
        options = options == null ? List.of() : List.copyOf(options);
        validateRange(min, max, "min must not be greater than max");
        validateRange(minLength, maxLength, "minLength must not be greater than maxLength");
    }

    private static <T extends Comparable<T>> void validateRange(T min, T max, String message) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
