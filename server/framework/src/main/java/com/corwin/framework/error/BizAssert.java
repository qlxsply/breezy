package com.corwin.framework.error;

import java.util.Collection;

/**
 * Assertion utility that throws {@link BizException} when a business precondition is not met.
 *
 * @author Corwin 2026/3/30
 */
public abstract class BizAssert {

    private BizAssert() {
    }

    /**
     * Asserts that the expression is {@code true}.
     */
    public static void state(boolean expression, ErrorCode errorCode) {
        if (!expression) {
            throw new BizException(errorCode);
        }
    }

    /**
     * Asserts that the object is not {@code null}.
     */
    public static void notNull(Object obj, ErrorCode errorCode) {
        if (obj == null) {
            throw new BizException(errorCode);
        }
    }

    /**
     * Asserts that the string is not blank.
     */
    public static void notBlank(String s, ErrorCode errorCode) {
        if (s == null || s.trim().isEmpty()) {
            throw new BizException(errorCode);
        }
    }

    /**
     * Asserts that the collection is not empty.
     */
    public static <T> void notEmpty(Collection<T> collection, ErrorCode errorCode) {
        if (collection == null || collection.isEmpty()) {
            throw new BizException(errorCode);
        }
    }

    /**
     * Throws a {@link BizException} immediately.
     */
    public static void fail(ErrorCode errorCode) {
        throw new BizException(errorCode);
    }

    /**
     * Throws a {@link BizException} with a custom message (overrides the default {@code msg}).
     */
    public static void fail(ErrorCode errorCode, String message) {
        throw new BizException(message, errorCode);
    }

}
