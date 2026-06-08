package com.corwin.datasource.interfaces.web.req;

import com.corwin.datasource.domain.model.DatabaseType;
import com.corwin.framework.web.request.PageRuleRequest;
import com.corwin.framework.web.request.SortRuleRequest;

/**
 * @author Corwin 2026/1/11
 */
public record ConnectionPageReq(
        PageRuleRequest page,
        SortRuleRequest sort,
        String nameLike,
        DatabaseType dbType
) {
}
