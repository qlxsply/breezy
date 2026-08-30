package com.corwin.datasource.interfaces.web.res;

import com.corwin.framework.json.JsonLongString;

/**
 * @author Corwin 2026/1/11
 */
public record ColumnRes(
    @JsonLongString Long id,
    @JsonLongString Long databaseId,
    @JsonLongString Long tableId,
    String columnName,
    String typeName,
    Integer jdbcType,
    Integer columnSize,
    Integer decimalDigits,
    Boolean nullable,
    Integer ordinalPosition,
    String defaultValue,
    String remarkDb,
    String remarkCustom,
    String alias) {}
