package com.corwin.system.role.interfaces.web.req;

import java.util.List;

/**
 * Request DTO for updating role resource grants.
 *
 * @author Corwin 2026/5/19
 */
public record UpdateRoleGrantReq(List<Long> resourceIds) {}
