package com.corwin.system.diagnostic.domain.model;

/**
 * Enumerates the types of diagnostic events that can be emitted during a session.
 *
 * @author Corwin 2026/4/16
 */
public enum DiagnosticEventType {
  HTTP_SLOW_REQUEST,
  HTTP_ERROR_REQUEST,
  SQL_SLOW,
  SQL_ERROR,
  JFR_GC,
  JFR_EXCEPTION,
  JFR_THREAD_PARK,
  JFR_MONITOR_BLOCKED
}
