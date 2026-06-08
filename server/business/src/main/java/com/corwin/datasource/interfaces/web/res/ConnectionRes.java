package com.corwin.datasource.interfaces.web.res;

import com.corwin.datasource.domain.model.AuthMode;
import com.corwin.datasource.domain.model.ConnectMode;
import com.corwin.datasource.domain.model.DatabaseSourceSourceType;
import com.corwin.datasource.domain.model.DatabaseType;
import com.corwin.framework.json.JsonLongString;

import java.time.Instant;

/**
 * @author Corwin 2026/1/11
 */
public record ConnectionRes(
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
        String defaultSchema,
        String remarkCustom,
        String extraParams,
        DatabaseSourceSourceType sourceType,
        String status,
        Instant lastTestTime,
        Instant lastOkTime,
        String lastError
) {
}
