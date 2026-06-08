package com.corwin.system.config.application.config;

import com.corwin.framework.config.ConfigDefinition;
import com.corwin.framework.config.ConfigLevel;
import com.corwin.framework.config.ConfigValueType;
import com.corwin.framework.config.DefaultConfigKeys;

/**
 * 系统配置枚举定义。
 *
 * @author Corwin 2026/4/16
 */
public enum SystemConfigKeys implements ConfigDefinition {
    ACCESS_TOKEN_TTL_SECONDS("访问令牌有效期（秒）", ConfigValueType.LONG),
    ACCESS_TOKEN_SECRET("HS256 访问令牌密钥", ConfigValueType.STR),
    PASSWORD_MIN_LENGTH("密码最小长度", ConfigValueType.INT),
    PASSWORD_REQUIRE_DIGIT("密码必须包含数字", ConfigValueType.BOOL),
    PASSWORD_REQUIRE_LETTER("密码必须包含字母", ConfigValueType.BOOL),
    PASSWORD_REQUIRE_UPPER("密码必须包含大写字母", ConfigValueType.BOOL),
    PASSWORD_REQUIRE_LOWER("密码必须包含小写字母", ConfigValueType.BOOL),
    PASSWORD_REQUIRE_SPECIAL("密码必须包含特殊字符", ConfigValueType.BOOL),
    PASSWORD_FORCE_CHANGE_ON_FIRST_LOGIN("首次登录强制修改密码", ConfigValueType.BOOL),
    PASSWORD_FORCE_CHANGE_ON_RESET("重置密码后强制修改密码", ConfigValueType.BOOL),
    AUDIT_ENABLED("审计开关", ConfigValueType.BOOL),
    AUDIT_ASYNC_ENABLED("审计异步开关", ConfigValueType.BOOL),
    AUDIT_RECORD_REQUEST_MAX_LENGTH("审计请求摘要最大长度", ConfigValueType.INT),
    AUDIT_RECORD_RESPONSE_MAX_LENGTH("审计响应摘要最大长度", ConfigValueType.INT),
    STORAGE_BASE_PATH("存储根路径", ConfigValueType.STR),
    SYSTEM_FILE_PREVIEW_MAX_SIZE("系统文件预览最大大小", ConfigValueType.LONG),
    SSE_TICKET_TTL_SECONDS("SSE 凭证有效期（秒）", ConfigValueType.LONG),
    WEB_PUSH_VAPID_PUBLIC_KEY("Web Push VAPID 公钥", ConfigValueType.STR),
    WEB_PUSH_VAPID_PRIVATE_KEY("Web Push VAPID 私钥", ConfigValueType.STR),
    WEB_PUSH_VAPID_SUBJECT("Web Push VAPID 主题", ConfigValueType.STR),
    MSG_TYPE_CONFIGS("消息类型配置", ConfigValueType.STR);

    private final String desc;
    private final ConfigValueType type;
    private final ConfigLevel level;

    SystemConfigKeys(String desc, ConfigValueType type) {
        this(desc, type, ConfigLevel.SYSTEM);
    }

    SystemConfigKeys(String desc, ConfigValueType type, ConfigLevel level) {
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
        for (SystemConfigKeys item : values()) {
            if (item.name().equals(normalized)) {
                return item;
            }
        }
        return null;
    }
}
