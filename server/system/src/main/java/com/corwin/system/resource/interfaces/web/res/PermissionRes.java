package com.corwin.system.resource.interfaces.web.res;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.json.JsonLongString;

/**
 * @author Corwin 2026/4/20
 */
public record PermissionRes(
        @JsonLongString
        Long id,
        String code,
        String name,
        UserType userScope
) {
}
