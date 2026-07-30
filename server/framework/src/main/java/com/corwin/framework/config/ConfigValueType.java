package com.corwin.framework.config;

import com.corwin.framework.dict.DictEnumDefinition;

/**
 * Supported config value types.
 *
 * @author Corwin 2025/10/19
 */
public enum ConfigValueType implements DictEnumDefinition {
    INT("整数"),
    LONG("长整数"),
    BOOL("布尔"),
    DEC("小数"),
    STR("文本"),
    STR_LIST("字符串列表"),
    STR_SET("字符串集合");

    private final String label;

    ConfigValueType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}

