package com.corwin.framework.mybatis.pagination.dialect;

import java.util.List;

/**
 * A paginated SQL string together with its appended parameters (e.g. limit, offset).
 *
 * @param sql        the paginated SQL
 * @param parameters the appended pagination parameters
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
