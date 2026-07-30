package com.corwin.system.methodstat.domain.repo;

import com.corwin.system.methodstat.domain.model.MethodStatAggregateSnapshot;
import com.corwin.system.methodstat.domain.model.MethodStatKey;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link MethodStatAggregateSnapshot} persistence.
 * @author Corwin 2026/3/25
 */
public interface MethodStatSnapshotRepository {

    /**
     * Save a method statistics snapshot.
     * @param snapshot the snapshot to persist
     * @return the persisted snapshot
     */
    MethodStatAggregateSnapshot save(MethodStatAggregateSnapshot snapshot);

    /**
     * Find a snapshot by its method key.
     * @param key the method key
     * @return optional containing the snapshot if found
     */
    Optional<MethodStatAggregateSnapshot> findByKey(MethodStatKey key);

    /**
     * Return all stored snapshots.
     * @return list of all snapshots
     */
    List<MethodStatAggregateSnapshot> findAll();

    /**
     * Remove all snapshots from storage.
     */
    void clearAll();
}
