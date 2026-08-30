package com.corwin.framework.event.handler;

import com.corwin.framework.event.model.AsyncEvent;

/**
 * V1 interface-based event handler (kept for compatibility during migration).
 *
 * <p>New code should use {@code @AsyncEventListener} annotation instead.
 *
 * @author Corwin 2026/3/31
 */
public interface AsyncEventHandler<T extends AsyncEvent> {

  /** Returns the event type this handler supports. */
  Class<T> eventType();

  /** Handles the event. */
  void onEvent(T event);

  /** Execution order; lower values have higher priority. */
  default int order() {
    return 0;
  }
}
