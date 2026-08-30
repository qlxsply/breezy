package com.corwin.system.methodstat.infrastructure.persistence;

import com.corwin.system.methodstat.domain.model.MethodStatAggregate;
import com.corwin.system.methodstat.domain.model.MethodStatKey;
import com.corwin.system.methodstat.domain.repo.MethodStatAggregateRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/**
 * In-memory implementation of {@link MethodStatAggregateRepository} using ConcurrentHashMap.
 *
 * @author Corwin 2026/3/25
 */
@Repository
public class InMemoryMethodStatAggregateRepository implements MethodStatAggregateRepository {

  private final Map<String, MethodStatAggregate> store = new ConcurrentHashMap<>();

  /**
   * Retrieve an existing aggregate for the given key or create a new one.
   *
   * @param key the method key
   * @return the existing or newly created aggregate
   */
  @Override
  public MethodStatAggregate getOrCreate(MethodStatKey key) {
    return store.computeIfAbsent(key.value(), _k -> new MethodStatAggregate(key));
  }

  /**
   * Find an aggregate by the method key.
   *
   * @param key the method key
   * @return optional containing the aggregate if found
   */
  @Override
  public Optional<MethodStatAggregate> findByKey(MethodStatKey key) {
    return Optional.ofNullable(store.get(key.value()));
  }

  /**
   * Return all stored aggregates sorted by key.
   *
   * @return list of all aggregates
   */
  @Override
  public List<MethodStatAggregate> findAll() {
    return store.values().stream()
        .sorted(Comparator.comparing(item -> item.key().value()))
        .toList();
  }

  /**
   * Remove the aggregate for the given key from storage.
   *
   * @param key the method key to remove
   */
  @Override
  public void removeByKey(MethodStatKey key) {
    store.remove(key.value());
  }

  /** Remove all stored aggregates. */
  @Override
  public void clearAll() {
    store.clear();
  }
}
