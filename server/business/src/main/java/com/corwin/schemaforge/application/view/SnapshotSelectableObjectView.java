package com.corwin.schemaforge.application.view;

/**
 * @author Corwin 2026/2/26
 */
public record SnapshotSelectableObjectView(
    Long id, String tableName, String tableSchema, String tableType, String alias) {}
