package com.corwin.datasource.application.view;

import com.corwin.datasource.domain.model.AuthMode;
import com.corwin.datasource.domain.model.ConnectMode;
import com.corwin.datasource.domain.model.DatabaseType;

/**
 * @author Corwin 2026/3/6
 */
public record DatabaseSourceConnectionInfoView(
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
    String remarkCustom,
    String extraParams) {}
