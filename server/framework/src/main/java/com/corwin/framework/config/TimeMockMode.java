package com.corwin.framework.config;

import com.corwin.framework.dict.DictEnumDefinition;

/**
 * 业务时间模拟模式。
 *
 * @author Corwin 2026/8/17
 */
public enum TimeMockMode implements DictEnumDefinition {

    DYNAMIC("动态偏移"),
    FIXED("固定时间");

    private final String label;

    TimeMockMode(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
