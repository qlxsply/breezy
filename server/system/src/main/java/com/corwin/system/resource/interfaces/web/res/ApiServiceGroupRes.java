package com.corwin.system.resource.interfaces.web.res;

import java.util.List;

/**
 * Response DTO for grouping APIs by service.
 *
 * @author Corwin 2026/1/23
 */
public record ApiServiceGroupRes(String service, List<ApiModuleGroupRes> modules) {}
