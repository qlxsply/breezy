package com.corwin.system.config.interfaces.web.res;

import com.corwin.framework.config.ConfigLevel;
import com.corwin.framework.config.ConfigScope;
import com.corwin.framework.config.ConfigValueType;

/**
 * Response DTO for a single configuration entry.
 * Exposes code, scope, description, value type, current value, level,
 * and whether the config supports personalization.
 *
 * @author Corwin 2026/5/5
 */
public record ConfigRes(
        String code,
        ConfigScope scope,
        String description,
        ConfigValueType valueType,
        String value,
        ConfigLevel level,
        boolean personalized
) {
}
