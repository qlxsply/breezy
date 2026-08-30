package com.corwin.datasource.interfaces.web.res;

import com.corwin.framework.json.JsonLongString;

/**
 * @author Corwin 2026/1/11
 */
public record TableRes(
    @JsonLongString Long id,
    @JsonLongString Long databaseId,
    String tableCatalog,
    String tableSchema,
    String tableName,
    String tableType,
    String remarkDb,
    String remarkCustom,
    String alias) {}
