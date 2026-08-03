package com.corwin.system.config.application.view;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 配置有效值查询视图，供前端按 configKey 读取系统配置结果。
 *
 * @author Corwin 2026/7/31
 */
public record ConfigEffectiveView(
        String key,
        String title,
        String description,
        JsonNode effectiveValue
) {

    public ConfigEffectiveView {
        effectiveValue = effectiveValue.deepCopy();
    }
}
