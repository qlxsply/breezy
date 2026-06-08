package com.corwin.jsonfmt.application.view;

import java.time.Instant;

/**
 * @author Corwin 2026/3/2
 */
public record JsonFmtRecordListItemView(
        String id,
        String name,
        Integer orderNo,
        Instant updatedAt
) {
}
