package com.corwin.system.methodstat.infrastructure.persistence;

import com.corwin.system.methodstat.domain.model.MethodStatKey;
import com.corwin.system.methodstat.domain.model.MethodStatMetadata;
import com.corwin.system.methodstat.domain.repo.MethodStatMetadataRepository;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link MethodStatMetadataRepository} using ConcurrentHashMap.
 * @author Corwin 2026/3/25
 */
@Repository
public class InMemoryMethodStatMetadataRepository implements MethodStatMetadataRepository {

    private final Map<String, MethodStatMetadata> store = new ConcurrentHashMap<>();

    /**
     * Save or update the metadata for a method.
     * @param metadata the metadata to store
     * @return the stored metadata
     */
    @Override
    public MethodStatMetadata save(MethodStatMetadata metadata) {
        store.put(metadata.key().value(), metadata);
        return metadata;
    }

    /**
     * Find metadata by the method key.
     * @param key the method key
     * @return optional containing the metadata if found
     */
    @Override
    public Optional<MethodStatMetadata> findByKey(MethodStatKey key) {
        return Optional.ofNullable(store.get(key.value()));
    }

    /**
     * Return all stored metadata entries sorted by key.
     * @return list of all metadata
     */
    @Override
    public List<MethodStatMetadata> findAll() {
        return store.values().stream().sorted(Comparator.comparing(item -> item.key().value())).toList();
    }

    /**
     * Remove all stored metadata entries.
     */
    @Override
    public void clearAll() {
        store.clear();
    }
}
