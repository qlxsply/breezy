package com.corwin.datasource.interfaces.web.res;

import com.corwin.datasource.domain.model.DatabaseType;

/**
 * @author Corwin 2026/2/26
 */
public record DatabaseSchemaBasicInfoRes(
        String dataSourceName,
        DatabaseType dbType,
        String jdbcUrl,
        String username,
        String passwordRaw
) {
}
