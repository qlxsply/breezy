package com.corwin.datasource.interfaces.web.res;

import com.corwin.framework.json.JsonLongString;

import java.time.Instant;

/**
 * @author Corwin 2026/1/11
 */
public record CatalogRes(
        @JsonLongString
        Long id,
        @JsonLongString
        Long dataSourceId,
        String databaseName,
        String alias,
        String remarkCustom,
        String productName,
        String productVersion,
        String driverName,
        String driverVersion,
        Instant fetchedAt,
        Instant createdAt
) {
}
