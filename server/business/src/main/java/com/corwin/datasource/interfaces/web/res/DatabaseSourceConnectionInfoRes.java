package com.corwin.datasource.interfaces.web.res;

import com.corwin.datasource.domain.model.AuthMode;
import com.corwin.datasource.domain.model.ConnectMode;
import com.corwin.datasource.domain.model.DatabaseType;
import com.corwin.framework.json.JsonLongString;

/**
 * @author Corwin 2026/3/6
 */
public record DatabaseSourceConnectionInfoRes(
        @JsonLongString
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
        String extraParams
) {
}
