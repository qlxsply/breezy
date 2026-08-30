package com.corwin.system.resource.interfaces.web.res;

import com.corwin.framework.json.JsonLongString;
import com.corwin.system.resource.domain.model.ApiAccessType;
import com.corwin.system.resource.domain.model.ApiMethod;
import com.corwin.system.resource.domain.model.ApiProtocol;

/**
 * Response DTO for API metadata with security and audit attributes.
 *
 * @author Corwin 2026/1/23
 */
public record ApiRes(
    @JsonLongString Long id,
    String module,
    ApiProtocol protocol,
    ApiMethod httpMethod,
    String pathPattern,
    String handlerClass,
    String handlerMethod,
    boolean permissionDeclared,
    ApiAccessType accessType,
    String userType,
    boolean auditDeclared,
    String auditResource,
    String auditAction,
    String auditDescription,
    String sortOptionsJson,
    boolean enabled,
    int referenceCount,
    boolean referenced) {}
