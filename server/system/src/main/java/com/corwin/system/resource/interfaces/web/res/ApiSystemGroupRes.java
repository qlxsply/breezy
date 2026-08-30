package com.corwin.system.resource.interfaces.web.res;

import java.util.List;

/**
 * Response DTO for grouping APIs by system.
 *
 * @author Corwin 2026/1/23
 */
public record ApiSystemGroupRes(String system, List<ApiServiceGroupRes> services) {}
