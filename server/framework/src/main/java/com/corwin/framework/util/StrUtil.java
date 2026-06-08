package com.corwin.framework.util;

import com.corwin.framework.constant.TextConstants;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Corwin 2026/1/11
 */
public final class StrUtil {

    private StrUtil() {
    }

    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static boolean isNotEmpty(String s) {
        return !isEmpty(s);
    }

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public static boolean isNotBlank(String s) {
        return !isBlank(s);
    }

    public static String nullToEmpty(String s) {
        return s == null ? TextConstants.EMPTY : s;
    }

    public static String emptyToNull(String s) {
        return (s == null || s.isEmpty()) ? null : s;
    }

    public static String trimToEmpty(String s) {
        return s == null ? TextConstants.EMPTY : s.trim();
    }

    public static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String defaultIfNull(String s, String defaultVal) {
        return s == null ? defaultVal : s;
    }

    public static String defaultIfBlank(String s, String defaultVal) {
        return isBlank(s) ? defaultVal : s;
    }

    public static boolean equals(String a, String b) {
        return Objects.equals(a, b);
    }

    public static boolean equalsIgnoreCase(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equalsIgnoreCase(b);
    }

    public static boolean contains(String s, String search) {
        return s != null && search != null && s.contains(search);
    }

    public static String join(String... items) {
        return join(TextConstants.COMMA, items);
    }

    public static String join(String delimiter, String... items) {
        Objects.requireNonNull(delimiter, "delimiter must not be null");
        Objects.requireNonNull(items, "items array must not be null");
        if (items.length == 0) {
            return TextConstants.EMPTY;
        }
        return String.join(delimiter, items);
    }

    public static <T> String join(Collection<T> items, Function<T, String> mapper) {
        return join(items, mapper, TextConstants.COMMA);
    }

    public static <T> String join(Collection<T> items, Function<T, String> mapper, String delimiter) {
        Objects.requireNonNull(items, "items collection must not be null");
        Objects.requireNonNull(mapper, "mapper function must not be null");
        Objects.requireNonNull(delimiter, "delimiter must not be null");
        if (items.isEmpty()) {
            return TextConstants.EMPTY;
        }
        return items.stream().map(item -> Objects.toString(mapper.apply(item), TextConstants.EMPTY))
                    .collect(Collectors.joining(delimiter));
    }

    public static String[] split(String str) {
        return split(str, TextConstants.COMMA);
    }

    public static String[] split(String str, String delimiter) {
        Objects.requireNonNull(str, "input string must not be null");
        Objects.requireNonNull(delimiter, "delimiter must not be null");
        if (str.isEmpty()) {
            return new String[]{};
        }
        String[] parts = str.split(Pattern.quote(delimiter), -1);
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        return parts;
    }

    public static String getAtOrNull(String str, int index) {
        return getAtOrNull(str, TextConstants.COMMA, index);
    }

    public static String getAtOrNull(String str, String delimiter, int index) {
        String[] parts = split(str, delimiter);
        return (index >= 0 && index < parts.length) ? parts[index] : null;
    }
}
