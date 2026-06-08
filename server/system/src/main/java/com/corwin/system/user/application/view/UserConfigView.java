package com.corwin.system.user.application.view;

import com.corwin.framework.config.ConfigValueType;

/**
 * @author Corwin 2026/3/30
 */
public record UserConfigView(
        String code,
        String description,
        ConfigValueType valueType,
        String value
) {
}
