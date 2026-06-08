package com.corwin.framework.error;

import java.util.Objects;

/**
 * 错误码校验工具。
 *
 * @author Corwin 2026/3/30
 */
public final class ErrorCodes {

    private ErrorCodes() {
    }

    public static void validate(String code, String msg, ErrorCodeRange range) {
        Objects.requireNonNull(range, "ErrorCodeRange must not be null");

        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Error code must not be blank");
        }
        if (!code.matches("\\d{6}")) {
            throw new IllegalArgumentException("Error code must be 6-digit numeric string: " + code);
        }
        if (msg == null || msg.isBlank()) {
            throw new IllegalArgumentException("Error message must not be blank");
        }
        if (!range.contains(code)) {
            String start = range.getStart();
            String end = range.getEnd();
            String name = range.getName();
            String s = String.format("Error code [%s] is out of range [%s-%s] for range [%s]", code, start, end, name);
            throw new IllegalArgumentException(s);
        }
    }

}