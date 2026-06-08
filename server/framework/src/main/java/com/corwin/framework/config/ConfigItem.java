package com.corwin.framework.config;

/**
 *
 * @author Corwin 2025/10/14
 */
public final class ConfigItem {
    /**
     * 配置项键，{@link DefaultConfigKeys}
     */
    public String key;
    /**
     * 配置项值
     */
    public String value;
    /**
     * 配置项值类型
     */
    public ConfigValueType type;
}

