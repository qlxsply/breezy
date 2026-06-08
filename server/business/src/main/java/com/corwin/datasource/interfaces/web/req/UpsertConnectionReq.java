package com.corwin.datasource.interfaces.web.req;

import com.corwin.datasource.domain.model.AuthMode;
import com.corwin.datasource.domain.model.ConnectMode;
import com.corwin.datasource.domain.model.DatabaseType;
import com.corwin.framework.json.JsonLongString;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Corwin 2026/1/11
 */
@Getter
@Setter
public class UpsertConnectionReq {
    @JsonLongString
    private Long id;
    private String name;
    private DatabaseType dbType;
    private AuthMode authMode;
    private ConnectMode connectMode;
    private String host;
    private Integer port;
    private String databaseName;
    private String serviceName;
    private String sid;
    private String driverClassName;
    private String jdbcUrl;
    private String username;
    private String passwordRaw;
    private String defaultSchema;
    private String remarkCustom;
    private String extraParams;
}
