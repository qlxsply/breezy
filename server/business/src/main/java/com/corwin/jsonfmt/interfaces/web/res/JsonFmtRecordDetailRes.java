package com.corwin.jsonfmt.interfaces.web.res;

import java.time.Instant;

/**
 * @author Corwin 2026/3/2
 */
public record JsonFmtRecordDetailRes(
        String id,
        String name,
        String content,
        Integer orderNo,
        Instant createdAt,
        Instant updatedAt
) {
}
