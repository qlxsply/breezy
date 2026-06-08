package com.corwin.schemaforge.interfaces.web.res;

/**
 * @author Corwin 2026/2/24
 */
public record DiffResultRes(
        int addedCount,
        int removedCount,
        int changedCount,
        String changeLogXml,
        String changeSql
) {
}
