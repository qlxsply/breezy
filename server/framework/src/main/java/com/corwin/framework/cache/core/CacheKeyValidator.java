package com.corwin.framework.cache.core;

import com.corwin.framework.cache.config.FrameworkCacheProperties;
import com.corwin.framework.error.BizException;
import com.corwin.framework.error.CacheError;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;

/**
 * Validates cache operation parameters (key length, field presence, TTL, value type).
 *
 * @author Corwin 2026/4/19
 */
public class CacheKeyValidator {

  private final FrameworkCacheProperties properties;

  public CacheKeyValidator(FrameworkCacheProperties properties) {
    this.properties = properties;
  }

  public void validateKey(String key) {
    if (key == null) {
      throw new BizException(CacheError.CACHE_KEY_REQUIRED);
    }
    if (!properties.getKey().isAllowEmpty() && key.trim().isEmpty()) {
      throw new BizException(CacheError.CACHE_KEY_REQUIRED);
    }
    if (key.length() > properties.getKey().getMaxLength()) {
      int maxLength = properties.getKey().getMaxLength();
      String msg = String.format("Cache key length exceeds max length: %s", maxLength);
      throw new BizException(msg, CacheError.CACHE_KEY_TOO_LONG);
    }
  }

  public void validateField(String field) {
    if (field == null || field.trim().isEmpty()) {
      throw new BizException(CacheError.CACHE_FIELD_REQUIRED);
    }
  }

  public void validateTtl(Duration ttl) {
    if (ttl == null || ttl.isZero() || ttl.isNegative()) {
      throw new BizException(CacheError.CACHE_TTL_INVALID);
    }
  }

  public <T> Class<T> validateValueType(Class<T> valueType) {
    if (valueType == null) {
      throw new BizException(CacheError.CACHE_VALUE_TYPE_REQUIRED);
    }
    return valueType;
  }

  public void validateValue(Object value) {
    if (value == null) {
      throw new BizException(CacheError.CACHE_VALUE_REQUIRED);
    }
  }

  public void validateKeys(Collection<String> keys) {
    if (keys == null) {
      throw new BizException(CacheError.CACHE_VALUE_REQUIRED);
    }
    for (String key : keys) {
      validateKey(key);
    }
  }

  public void validateValues(Collection<?> values) {
    if (values == null) {
      throw new BizException(CacheError.CACHE_VALUE_REQUIRED);
    }
    for (Object value : values) {
      validateValue(value);
    }
  }

  public void validateMap(Map<?, ?> values) {
    if (values == null) {
      throw new BizException(CacheError.CACHE_VALUE_REQUIRED);
    }
  }
}
