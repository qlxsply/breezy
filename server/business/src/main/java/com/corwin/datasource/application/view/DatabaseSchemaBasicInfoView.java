package com.corwin.datasource.application.view;

import com.corwin.datasource.domain.model.DatabaseType;

/**
 * @author Corwin 2026/2/26
 */
public record DatabaseSchemaBasicInfoView(
        String dataSourceName,
        DatabaseType dbType,
        String jdbcUrl,
        String username,
        String passwordRaw) {
}
