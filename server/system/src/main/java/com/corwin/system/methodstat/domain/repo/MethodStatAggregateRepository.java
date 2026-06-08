package com.corwin.system.methodstat.domain.repo;

import com.corwin.system.methodstat.domain.model.MethodStatAggregate;
import com.corwin.system.methodstat.domain.model.MethodStatKey;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/3/25
 */
public interface MethodStatAggregateRepository {

    MethodStatAggregate getOrCreate(MethodStatKey key);

    Optional<MethodStatAggregate> findByKey(MethodStatKey key);

    List<MethodStatAggregate> findAll();

    void removeByKey(MethodStatKey key);

    void clearAll();
}
