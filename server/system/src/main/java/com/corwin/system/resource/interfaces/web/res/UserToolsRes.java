package com.corwin.system.resource.interfaces.web.res;

import java.util.List;

/**
 * Response DTO containing the current user's available tools and permission codes.
 *
 * @author Corwin 2026/6/6
 */
public record UserToolsRes(List<UserToolPageRes> tools, List<String> permissionCodes) {}
