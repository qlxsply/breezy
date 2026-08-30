package com.corwin.schemaforge.application.view;

/**
 * @author Corwin 2026/2/24
 */
public record DiffResultView(
    int addedCount, int removedCount, int changedCount, String changeLogXml, String changeSql) {}
