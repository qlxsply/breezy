package com.corwin.system.methodstat.domain.repo;

import com.corwin.system.methodstat.domain.model.MethodStatAggregate;
import com.corwin.system.methodstat.domain.model.MethodStatKey;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link MethodStatAggregate} persistence.
 * @author Corwin 2026/3/25
 */
public interface MethodStatAggregateRepository {

    /**
     * Retrieve an existing aggregate for the given key or create a new one.
     * @param key the method key
     * @return the existing or newly created aggregate
     */
    MethodStatAggregate getOrCreate(MethodStatKey key);

    /**
     * Find an aggregate by its method key.
     * @param key the method key
     * @return optional containing the aggregate if found
     */
    Optional<MethodStatAggregate> findByKey(MethodStatKey key);

    /**
     * Return all stored aggregates.
     * @return list of all aggregates
     */
    List<MethodStatAggregate> findAll();

    /**
     * Remove the aggregate for the given key from storage.
     * @param key the method key to remove
     */
    void removeByKey(MethodStatKey key);

    /**
     * Remove all aggregates from storage.
     */
    void clearAll();
}
