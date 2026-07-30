package com.corwin.system.resource.interfaces.web.res;

import com.corwin.system.resource.domain.model.ResourceType;

import java.util.List;

/**
 * Response DTO representing a node in the system resource tree.
 *
 * <p>Supports recursive children to represent the full tree hierarchy.</p>
 *
 * @author Corwin 2026/6/29
 */
public record SystemResourceTreeItemRes(
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
        List<String> permissionIds,
        List<SystemResourceTreeItemRes> children
) {
}
