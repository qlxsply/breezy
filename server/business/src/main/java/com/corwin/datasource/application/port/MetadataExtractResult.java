package com.corwin.datasource.application.port;

import java.time.Instant;
import java.util.List;

/**
 * @author Corwin 2026/1/11
 */
public record MetadataExtractResult(
        BaseInfo baseInfo,
        List<TableInfo> tables,
        List<ColumnInfo> columns
) {
    public record BaseInfo(
            String productName,
            String productVersion,
            String driverName,
            String driverVersion,
            String jdbcUrl,
            String dbUser,
            Instant fetchedAt
    ) {
    }

    public record TableInfo(
            String catalog,
            String schema,
            String name,
            String type,
            String remarkDb
    ) {
    }

    public record ColumnInfo(
            String tableCatalog,
            String tableSchema,
            String tableName,
            String columnName,
            String typeName,
            Integer jdbcType,
            Integer columnSize,
            Integer decimalDigits,
            Boolean nullable,
            Integer ordinalPosition,
            String defaultValue,
            String remarkDb
    ) {
    }
}
