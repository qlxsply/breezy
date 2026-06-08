package com.corwin.framework.error;

import java.util.Collection;

/**
 * 业务断言工具：用于快速校验业务前置条件，不满足则抛 BizException。
 *
 * @author Corwin 2026/3/30
 */
public abstract class BizAssert {

    private BizAssert() {
    }

    /**
     * 条件必须为 true，否则抛业务异常
     */
    public static void state(boolean expression, ErrorCode errorCode) {
        if (!expression) {
            throw new BizException(errorCode);
        }
    }

    /**
     * 对象必须非 null，否则抛业务异常
     */
    public static void notNull(Object obj, ErrorCode errorCode) {
        if (obj == null) {
            throw new BizException(errorCode);
        }
    }

    /**
     * 字符串必须非空白，否则抛业务异常
     */
    public static void notBlank(String s, ErrorCode errorCode) {
        if (s == null || s.trim().isEmpty()) {
            throw new BizException(errorCode);
        }
    }

    /**
     * 集合必须非空，否则抛业务异常
     */
    public static <T> void notEmpty(Collection<T> collection, ErrorCode errorCode) {
        if (collection == null || collection.isEmpty()) {
            throw new BizException(errorCode);
        }
    }

    /**
     * 直接抛业务异常
     */
    public static void fail(ErrorCode errorCode) {
        throw new BizException(errorCode);
    }

    /**
     * 直接抛业务异常（自定义消息覆盖默认 msg）
     */
    public static void fail(ErrorCode errorCode, String message) {
        throw new BizException(message, errorCode);
    }

}
