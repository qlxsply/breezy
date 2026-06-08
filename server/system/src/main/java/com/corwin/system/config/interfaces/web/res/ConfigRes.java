package com.corwin.system.config.interfaces.web.res;

import com.corwin.framework.config.ConfigLevel;
import com.corwin.framework.config.ConfigScope;
import com.corwin.framework.config.ConfigValueType;

/**
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
