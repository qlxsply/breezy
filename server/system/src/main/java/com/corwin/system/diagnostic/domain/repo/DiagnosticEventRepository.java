package com.corwin.system.diagnostic.domain.repo;

import com.corwin.system.diagnostic.domain.model.DiagnosticEvent;
import java.util.List;

/**
 * Repository interface for storing and retrieving diagnostic events.
 *
 * @author Corwin 2026/4/16
 */
public interface DiagnosticEventRepository {

  /**
   * Configures the maximum number of events to retain.
   *
   * @param capacity the new capacity
   */
  void configureCapacity(int capacity);

  /**
   * Appends a new event to the repository.
   *
   * @param event the event to append
   */
  void append(DiagnosticEvent event);

  /**
   * Returns the most recent events up to the given limit, in reverse chronological order.
   *
   * @param limit the maximum number of events to return
   * @return a list of recent events
   */
  List<DiagnosticEvent> latest(int limit);

  /** Removes all events from the repository. */
  void clear();
}
