package com.corwin.datasource.application.command;

import com.corwin.datasource.domain.model.AuthMode;
import com.corwin.datasource.domain.model.ConnectMode;
import com.corwin.datasource.domain.model.DatabaseType;

/**
 *
 * @author Corwin 2026/1/11
 */
public record UpsertConnectionCommand(
        Long id,
        String name,
        DatabaseType dbType,
        AuthMode authMode,
        ConnectMode connectMode,
        String host,
        Integer port,
        String databaseName,
        String serviceName,
        String sid,
        String driverClassName,
        String jdbcUrl,
        String username,
        String passwordRaw,
        String defaultSchema,
        String remarkCustom,
        String extraParams
) {
}
