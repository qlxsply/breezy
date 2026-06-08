package com.corwin.system.methodstat.domain.repo;

import com.corwin.system.methodstat.domain.model.MethodStatAggregateSnapshot;
import com.corwin.system.methodstat.domain.model.MethodStatKey;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/3/25
 */
public interface MethodStatSnapshotRepository {

    MethodStatAggregateSnapshot save(MethodStatAggregateSnapshot snapshot);

    Optional<MethodStatAggregateSnapshot> findByKey(MethodStatKey key);

    List<MethodStatAggregateSnapshot> findAll();

    void clearAll();
}
