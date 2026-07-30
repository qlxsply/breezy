package com.corwin.framework.config;

/**
 * Built-in config key definitions with descriptions and value types.
 * <p>
 * Values are segregated by level: {@link ConfigLevel#SYSTEM} items apply globally;
 * {@link ConfigLevel#USER} items are personalizable per user.
 *
 * @author Corwin 2026/1/31
 */
public enum DefaultConfigKeys implements ConfigDefinition {
    // System-level configs
    TIME_OFFSET("System time offset (seconds)", ConfigValueType.LONG),
    CLIENT_IP_MODE("Client IP resolution mode", ConfigValueType.STR),
    AUTH_WHITELIST("Authentication whitelist", ConfigValueType.STR),
    LOGGING_FILTER_EXCLUDE_PREFIXES("Logging filter exclude path prefixes", ConfigValueType.STR_LIST),
    LOGGING_FILTER_STREAM_PREFIXES("Logging filter streaming response path prefixes", ConfigValueType.STR_LIST),
    // User-level (personalizable) configs
    USER_TIME_ZONE("User time zone", ConfigValueType.STR, ConfigLevel.USER),
    USER_DATE_TIME_FORMAT("User date-time format", ConfigValueType.STR, ConfigLevel.USER),
    USER_DATE_FORMAT("User date format", ConfigValueType.STR, ConfigLevel.USER),
    USER_DECIMAL_FORMAT("User decimal format", ConfigValueType.STR, ConfigLevel.USER),
    ;

    private final String desc;
    private final ConfigValueType type;
    private final ConfigLevel level;

    DefaultConfigKeys(String desc, ConfigValueType type) {
        this(desc, type, ConfigLevel.SYSTEM);
    }

    DefaultConfigKeys(String desc, ConfigValueType type, ConfigLevel level) {
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
        String normalized = code.trim();
        for (DefaultConfigKeys item : values()) {
            if (item.name().equals(normalized)) {
                return item;
            }
        }
        return null;
    }
}
