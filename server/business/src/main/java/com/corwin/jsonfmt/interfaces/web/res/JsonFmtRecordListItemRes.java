package com.corwin.jsonfmt.interfaces.web.res;

import java.time.Instant;

/**
 * @author Corwin 2026/3/2
 */
public record JsonFmtRecordListItemRes(
        String id,
        String name,
        Integer orderNo,
        Instant updatedAt
) {
}
