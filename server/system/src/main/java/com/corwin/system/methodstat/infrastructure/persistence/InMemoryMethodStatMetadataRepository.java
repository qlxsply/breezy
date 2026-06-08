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
 * @author Corwin 2026/3/25
 */
@Repository
public class InMemoryMethodStatMetadataRepository implements MethodStatMetadataRepository {

    private final Map<String, MethodStatMetadata> store = new ConcurrentHashMap<>();

    @Override
    public MethodStatMetadata save(MethodStatMetadata metadata) {
        store.put(metadata.key().value(), metadata);
        return metadata;
    }

    @Override
    public Optional<MethodStatMetadata> findByKey(MethodStatKey key) {
        return Optional.ofNullable(store.get(key.value()));
    }

    @Override
    public List<MethodStatMetadata> findAll() {
        return store.values().stream().sorted(Comparator.comparing(item -> item.key().value())).toList();
    }

    @Override
    public void clearAll() {
        store.clear();
    }
}
