package com.corwin.system.config.interfaces.web.res;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 配置有效值查询响应。
 *
 * @author Corwin 2026/7/31
 */
public record ConfigEffectiveRes(
        String key,
        String title,
        String description,
        JsonNode effectiveValue
) {

    public ConfigEffectiveRes {
        effectiveValue = effectiveValue.deepCopy();
    }
}
