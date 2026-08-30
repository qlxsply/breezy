package com.corwin.schemaforge.application.model;

import java.util.Set;

/**
 * 快照对象选择结果。
 *
 * @author Corwin 2026/4/15
 */
public record SnapshotObjectSelection(
    Set<String> selectedTableNames, Set<String> selectedViewNames) {

  public static SnapshotObjectSelection empty() {
    return new SnapshotObjectSelection(Set.of(), Set.of());
  }
}
