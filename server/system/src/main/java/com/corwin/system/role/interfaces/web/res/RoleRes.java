package com.corwin.system.role.interfaces.web.res;

import com.corwin.framework.json.JsonLongString;

import java.time.Instant;

/**
 * @author Corwin 2026/1/23
 */
public record RoleRes(
        String id,
        String code,
        String name,
        boolean enabled,
        @JsonLongString
        Long createdBy,
        Instant createdAt,
        @JsonLongString
        Long updatedBy,
        Instant updatedAt
) {
}
