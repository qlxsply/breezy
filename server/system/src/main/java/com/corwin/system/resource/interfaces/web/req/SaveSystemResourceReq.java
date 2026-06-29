package com.corwin.system.resource.interfaces.web.req;

import com.corwin.system.resource.domain.model.ResourceType;

/**
 * @author Corwin 2026/6/29
 */
public record SaveSystemResourceReq(
        Long parentId,
        String code,
        String name,
        ResourceType resourceType,
        String path,
        String component,
        String icon,
        Integer sortNo,
        Boolean visible,
        Boolean enabled,
        Boolean defaultEntry,
        Boolean systemBuiltin,
        String remark
) {
}
