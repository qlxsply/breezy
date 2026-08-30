package com.corwin.system.resource.domain.repo;

import com.corwin.framework.constant.UserType;
import com.corwin.system.resource.domain.model.ApiAccessType;

/**
 * Query object for paginated API search with dynamic filter criteria.
 *
 * @author Corwin 2026/6/21
 */
public record ApiPageQuery(
    String module,
    String pathPattern,
    String handlerClass,
    String handlerMethod,
    Boolean permissionDeclared,
    ApiAccessType accessType,
    UserType userType,
    Boolean auditDeclared,
    Boolean enabled) {}
