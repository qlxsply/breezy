package com.corwin.system.resource.application.view;

import com.corwin.system.resource.domain.model.ResourceType;

import java.util.List;

/**
 * View object containing the full detail of a system resource.
 *
 * @author Corwin 2026/6/29
 */
public record SystemResourceDetailView(
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
        List<Long> permissionIds
) {
}
