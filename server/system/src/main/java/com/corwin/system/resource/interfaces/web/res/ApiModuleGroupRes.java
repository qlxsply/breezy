package com.corwin.system.resource.interfaces.web.res;

import java.util.List;

/**
 * Response DTO for grouping APIs by module.
 *
 * @author Corwin 2026/1/23
 */
public record ApiModuleGroupRes(String module, List<ApiRes> apis) {}
