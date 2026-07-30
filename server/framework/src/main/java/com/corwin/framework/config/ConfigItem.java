package com.corwin.framework.config;

/**
 * A single configuration item with key, raw value, and value type.
 *
 * @author Corwin 2025/10/14
 */
public final class ConfigItem {
    /**
     * Config key; defined constants are listed in {@link DefaultConfigKeys}.
     */
    public String key;
    /**
     * Raw string value.
     */
    public String value;
    /**
     * Value type hint for parsing.
     */
    public ConfigValueType type;
}

