package com.corwin.framework.mybatis.pagination.dialect;

import java.util.List;

/**
 * 分页 SQL 及其追加参数。
 *
 * @author Corwin 2026/7/28
 */
public record PageSql(
        String sql,
        List<PageParameter> parameters
) {

    public PageSql {
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
    }
}
