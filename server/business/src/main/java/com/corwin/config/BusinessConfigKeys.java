package com.corwin.config;

import com.corwin.framework.config.ConfigDefinition;
import com.corwin.framework.config.ConfigLevel;
import com.corwin.framework.config.ConfigValueType;
import com.corwin.framework.config.DefaultConfigKeys;

/**
 * 业务配置枚举定义。
 *
 * @author Corwin 2026/5/5
 */
public enum BusinessConfigKeys implements ConfigDefinition {
    JSONFMT_CONTENT_FILE_THRESHOLD("JsonFmt 文件阈值", ConfigValueType.INT),
    SCHEMAFORGE_DDL_FORMAT_ENABLED("SchemaForge DDL 格式化开关", ConfigValueType.BOOL),
    SCHEMAFORGE_DDL_QUALIFIER_MODE("SchemaForge DDL 限定符模式", ConfigValueType.STR),
    ;

    private final String desc;
    private final ConfigValueType type;
    private final ConfigLevel level;

    BusinessConfigKeys(String desc, ConfigValueType type) {
        this(desc, type, ConfigLevel.SYSTEM);
    }

    BusinessConfigKeys(String desc, ConfigValueType type, ConfigLevel level) {
        this.desc = desc;
        this.type = type;
        this.level = level;
    }

    @Override
    public String desc() {
        return desc;
    }

    @Override
    public ConfigValueType valueType() {
        return type;
    }

    @Override
    public ConfigLevel level() {
        return level;
    }

    public static ConfigDefinition fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }

        ConfigDefinition frameworkDefinition = DefaultConfigKeys.fromCode(code);
        if (frameworkDefinition != null) {
            return frameworkDefinition;
        }

        String normalized = code.trim();
        for (BusinessConfigKeys item : values()) {
            if (item.name().equals(normalized)) {
                return item;
            }
        }
        return null;
    }

}
