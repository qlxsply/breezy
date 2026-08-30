package com.corwin.system.role.interfaces.web.res;

/**
 * Response DTO for a grantable resource in the role permission tree.
 *
 * @author Corwin 2026/5/7
 */
public record RoleGrantResourceRes(
    String id,
    String parentId,
    String resourceId,
    String name,
    String code,
    String type,
    String description,
    boolean enabled,
    boolean selectable,
    int orderNo) {}
