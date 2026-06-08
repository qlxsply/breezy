package com.corwin.system.resource.interfaces.web.res;

import com.corwin.framework.json.JsonLongString;
import com.corwin.system.resource.domain.model.PermissionUserScope;

/**
 * @author Corwin 2026/4/20
 */
public record PermissionRes(
        @JsonLongString
        Long id,
        String code,
        String name,
        PermissionUserScope userScope,
        String description,
        boolean enabled
) {
}
