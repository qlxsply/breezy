package com.corwin.system.diagnostic.domain.repo;

import com.corwin.system.diagnostic.domain.model.DiagnosticSnapshot;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for storing and retrieving diagnostic snapshots.
 *
 * @author Corwin 2026/4/16
 */
public interface DiagnosticSnapshotRepository {

  /**
   * Configures the maximum number of snapshots to retain.
   *
   * @param capacity the new capacity
   */
  void configureCapacity(int capacity);

  /**
   * Appends a new snapshot to the repository.
   *
   * @param snapshot the snapshot to append
   */
  void append(DiagnosticSnapshot snapshot);

  /**
   * Returns the most recently appended snapshot, if any.
   *
   * @return the latest snapshot wrapped in Optional
   */
  Optional<DiagnosticSnapshot> latest();

  /**
   * Returns the most recent snapshots up to the given limit, in reverse chronological order.
   *
   * @param limit the maximum number of snapshots to return
   * @return a list of recent snapshots
   */
  List<DiagnosticSnapshot> latest(int limit);

  /** Removes all snapshots from the repository. */
  void clear();
}
