package com.corwin.system.resource.interfaces.web.res;

import com.corwin.system.resource.domain.model.ResourceType;
import java.util.List;

/**
 * Response DTO containing the full detail of a system resource.
 *
 * @author Corwin 2026/6/29
 */
public record SystemResourceDetailRes(
    Long id,
    Long parentId,
    String code,
    String name,
    ResourceType resourceType,
    String path,
    String component,
    String icon,
    Integer sortNo,
    boolean visible,
    boolean enabled,
    boolean defaultEntry,
    boolean systemBuiltin,
    String remark,
    List<String> permissionIds) {}
