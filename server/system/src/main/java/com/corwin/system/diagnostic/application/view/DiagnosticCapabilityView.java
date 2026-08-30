package com.corwin.system.diagnostic.application.view;

import java.util.List;

/**
 * View object describing which diagnostic capabilities are available in the current runtime.
 *
 * @author Corwin 2026/4/16
 */
public record DiagnosticCapabilityView(
    boolean jfrAvailable,
    boolean httpAvailable,
    boolean dbPoolAvailable,
    boolean sqlAvailable,
    boolean deepModeSupported,
    List<String> dataSourceNames) {

  /**
   * Compact constructor that normalises a null data source names list to an immutable empty list.
   */
  public DiagnosticCapabilityView {
    dataSourceNames = dataSourceNames == null ? List.of() : List.copyOf(dataSourceNames);
  }
}
