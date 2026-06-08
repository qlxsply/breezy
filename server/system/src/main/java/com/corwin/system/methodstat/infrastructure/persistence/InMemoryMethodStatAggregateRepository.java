package com.corwin.system.methodstat.infrastructure.persistence;

import com.corwin.system.methodstat.domain.model.MethodStatAggregate;
import com.corwin.system.methodstat.domain.model.MethodStatKey;
import com.corwin.system.methodstat.domain.repo.MethodStatAggregateRepository;
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
public class InMemoryMethodStatAggregateRepository implements MethodStatAggregateRepository {

    private final Map<String, MethodStatAggregate> store = new ConcurrentHashMap<>();

    @Override
    public MethodStatAggregate getOrCreate(MethodStatKey key) {
        return store.computeIfAbsent(key.value(), _k -> new MethodStatAggregate(key));
    }

    @Override
    public Optional<MethodStatAggregate> findByKey(MethodStatKey key) {
        return Optional.ofNullable(store.get(key.value()));
    }

    @Override
    public List<MethodStatAggregate> findAll() {
        return store.values().stream().sorted(Comparator.comparing(item -> item.key().value())).toList();
    }

    @Override
    public void removeByKey(MethodStatKey key) {
        store.remove(key.value());
    }

    @Override
    public void clearAll() {
        store.clear();
    }
}
