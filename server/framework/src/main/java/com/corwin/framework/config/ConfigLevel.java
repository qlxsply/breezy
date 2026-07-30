package com.corwin.framework.config;

import com.corwin.framework.dict.DictEnumDefinition;

/**
 * Config level — whether a setting applies system-wide or can be personalized per user.
 *
 * @author Corwin 2026/3/30
 */
public enum ConfigLevel implements DictEnumDefinition {
    SYSTEM("系统级"),
    USER("用户级");

    private final String label;

    ConfigLevel(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
