package com.corwin.framework.cache.local;

import com.corwin.framework.cache.CacheDataType;
import com.corwin.framework.cache.CacheObjectOps;
import com.corwin.framework.cache.core.CacheEntry;
import com.corwin.framework.cache.core.CacheKeyValidator;
import com.corwin.framework.cache.support.TypeSafeValueCaster;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Local cache object (key-value) operations backed by the store.
 *
 * @author Corwin 2026/4/19
 */
public class LocalCacheObjectOps<T> extends AbstractLocalCacheOps implements CacheObjectOps<T> {

    private final Class<T> valueType;

    public LocalCacheObjectOps(LocalCacheStore store, CacheKeyValidator validator, Class<T> valueType) {
        super(store, validator, CacheDataType.OBJECT);
        this.valueType = validator.validateValueType(valueType);
    }

    @Override
    public Optional<T> get(String key) {
        validator.validateKey(key);
        return store.read(dataType(), key, entry -> Optional.of(TypeSafeValueCaster.cast(entry.getValue(), valueType)),
                Optional::empty);
    }

    @Override
    public void set(String key, T value) {
        set(key, value, null);
    }

    @Override
    public void set(String key, T value, Duration ttl) {
        validator.validateKey(key);
        validator.validateValue(value);
        if (ttl != null) {
            validator.validateTtl(ttl);
        }
        store.write(dataType(), key, (entry, now) -> LocalCacheWriteResult.keep(upsert(entry, value, now, ttl), null));
    }

    @Override
    public Map<String, T> multiGet(Collection<String> keys) {
        validator.validateKeys(keys);
        Map<String, T> result = new LinkedHashMap<>();
        for (String key : keys) {
            get(key).ifPresent(value -> result.put(key, value));
        }
        return result;
    }

    @Override
    public void multiSet(Map<String, T> values) {
        multiSet(values, null);
    }

    @Override
    public void multiSet(Map<String, T> values, Duration ttl) {
        validator.validateMap(values);
        if (ttl != null) {
            validator.validateTtl(ttl);
        }
        for (Map.Entry<String, T> entry : values.entrySet()) {
            set(entry.getKey(), entry.getValue(), ttl);
        }
    }

    private CacheEntry upsert(CacheEntry entry, T value, Instant now, Duration ttl) {
        Instant expireAt = ttl == null ? (entry == null ? null : entry.getExpireAt()) : now.plus(ttl);
        if (entry == null) {
            return CacheEntry.create(dataType(), value, now, expireAt);
        }
        return new CacheEntry(dataType(), value, entry.getCreatedAt(), now, expireAt);
    }
}
