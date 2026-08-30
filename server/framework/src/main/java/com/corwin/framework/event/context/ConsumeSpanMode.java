package com.corwin.framework.event.context;

/**
 * Span handling strategy for the consumption phase.
 *
 * @author Corwin 2026/4/9
 */
public enum ConsumeSpanMode {
  /** Reuses the snapshot span; does not create a child span. */
  REUSE,
  /** Creates a new child span under the snapshot's parent span during consumption. */
  NEW_CHILD
}
