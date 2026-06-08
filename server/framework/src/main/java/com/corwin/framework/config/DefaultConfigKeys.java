package com.corwin.framework.config;

/**
 * 默认配置枚举定义。
 *
 * @author Corwin 2026/1/31
 */
public enum DefaultConfigKeys implements ConfigDefinition {
    // 系统配置
    TIME_OFFSET("系统时间偏移量（秒）", ConfigValueType.LONG),
    CLIENT_IP_MODE("请求客户端 IP 获取方式", ConfigValueType.STR),
    AUTH_WHITELIST("认证白名单", ConfigValueType.STR),
    LOGGING_FILTER_EXCLUDE_PREFIXES("日志过滤器排除路径前缀", ConfigValueType.STR_LIST),
    LOGGING_FILTER_STREAM_PREFIXES("日志过滤器流式响应路径前缀", ConfigValueType.STR_LIST),
    // 用户配置
    USER_TIME_ZONE("用户时区", ConfigValueType.STR, ConfigLevel.USER),
    USER_DATE_TIME_FORMAT("用户日期时间格式", ConfigValueType.STR, ConfigLevel.USER),
    USER_DATE_FORMAT("用户日期格式", ConfigValueType.STR, ConfigLevel.USER),
    USER_DECIMAL_FORMAT("用户小数格式", ConfigValueType.STR, ConfigLevel.USER),
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
