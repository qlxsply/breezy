package com.corwin.jsonfmt.application.command;

import java.util.List;

/**
 * @author Corwin 2026/3/2
 */
public record JsonFmtRecordReorderCommand(
        List<String> orderedIds
) {
}
