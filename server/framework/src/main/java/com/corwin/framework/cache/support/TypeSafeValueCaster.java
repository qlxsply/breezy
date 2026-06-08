package com.corwin.framework.cache.support;

import com.corwin.framework.error.BizException;
import com.corwin.framework.error.CacheError;

/**
 * 缓存值类型转换工具。
 *
 * @author Corwin 2026/4/19
 */
public abstract class TypeSafeValueCaster {

    private TypeSafeValueCaster() {
    }

    public static <T> T cast(Object value, Class<T> valueType) {
        if (value == null) {
            return null;
        }
        if (!valueType.isInstance(value)) {
            String name = valueType.getName();
            String className = value.getClass().getName();
            String msg = String.format("Expected value type %s but got %s", name, className);
            throw new BizException(msg, CacheError.CACHE_VALUE_TYPE_MISMATCH);
        }
        return valueType.cast(value);
    }
}
