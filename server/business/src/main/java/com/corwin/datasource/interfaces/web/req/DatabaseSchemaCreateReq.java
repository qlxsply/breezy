package com.corwin.datasource.interfaces.web.req;

import com.corwin.framework.json.JsonLongString;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Corwin 2026/1/11
 */
@Getter
@Setter
public class DatabaseSchemaCreateReq {
    @JsonLongString
    private Long dataSourceId;
    private String databaseName;
    private String alias;
    private String remarkCustom;
}
