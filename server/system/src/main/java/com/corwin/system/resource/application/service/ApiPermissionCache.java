package com.corwin.system.resource.application.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * @author Corwin 2026/1/23
 */
@Component
public class ApiPermissionCache {

    private final Map<Long, Set<String>> cache = new ConcurrentHashMap<>();

    public Set<String> getOrLoad(Long key, Supplier<Set<String>> loader) {
        return cache.computeIfAbsent(key, __ -> loader.get());
    }

    public void clear(Long key) {
        cache.remove(key);
    }

    public void clearAll() {
        cache.clear();
    }
}
