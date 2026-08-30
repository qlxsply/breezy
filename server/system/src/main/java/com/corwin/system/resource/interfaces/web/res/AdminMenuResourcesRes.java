package com.corwin.system.resource.interfaces.web.res;

import java.util.List;

/**
 * Response DTO containing the admin's menu resource tree.
 *
 * @author Corwin 2026/5/31
 */
public record AdminMenuResourcesRes(List<AdminMenuResourceRes> resources) {}
