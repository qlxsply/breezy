package com.corwin.jsonfmt.application.view;

import java.time.Instant;

/**
 * @author Corwin 2026/3/2
 */
public record JsonFmtRecordDetailView(
    String id,
    String name,
    String content,
    Integer orderNo,
    Instant createdAt,
    Instant updatedAt) {}
