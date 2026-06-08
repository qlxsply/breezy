package com.corwin.datasource.interfaces.web.res;

import com.corwin.datasource.domain.model.DatabaseSourceSourceType;
import com.corwin.datasource.domain.model.DatabaseSourceStatus;
import com.corwin.datasource.domain.model.DatabaseType;
import com.corwin.framework.json.JsonLongString;

/**
 * 轻量级数据源信息，用于下拉选择
 *
 * @author Corwin 2026/2/6
 */
public record DatabaseSourceSimpleRes(
        @JsonLongString
        Long id,
        String name,
        DatabaseType dbType,
        String username,
        DatabaseSourceStatus status,
        DatabaseSourceSourceType sourceType
) {
}
