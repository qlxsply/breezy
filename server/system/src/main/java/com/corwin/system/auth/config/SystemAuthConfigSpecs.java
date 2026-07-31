package com.corwin.system.auth.config;

import com.corwin.framework.config.definition.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Corwin 2026/7/31
 */
public final class SystemAuthConfigSpecs {

    public static final ConfigSpec<AuthenticationConfig> AUTHENTICATION = new SimpleConfigSpec<>(
            new ConfigKey("system.security.authentication"), "system", "security", "认证策略",
            "内部会话和外部令牌有效期策略", AuthenticationConfig.class,
            new AuthenticationConfig(28800, false, 300, 60, 1800, 604800, 60, "breezy"), 1, 10,
            ConfigActivationPolicy.DYNAMIC, ConfigEditPolicy.ADMIN_EDITABLE, ConfigInvalidValuePolicy.FAIL_STARTUP,
            List.of(longField("internalSessionTtlSeconds", "内部会话有效秒数", 10),
                    boolField("internalSingleLoginEnabled", "内部账号单点登录", 20),
                    longField("sessionCacheTtlSeconds", "会话缓存秒数", 30),
                    longField("lastAccessRefreshIntervalSeconds", "访问时间刷新间隔秒数", 40),
                    longField("externalAccessTokenTtlSeconds", "访问令牌有效秒数", 50),
                    longField("externalRefreshTokenTtlSeconds", "刷新令牌有效秒数", 60),
                    longField("externalAccessTokenRefreshSkewSeconds", "令牌提前刷新秒数", 70),
                    stringField("externalJwtIssuer", "JWT 签发者", 80)), "default",
            SystemAuthConfigSpecs::validateAuthentication, SystemAuthConfigSpecs::validateAuthentication);

    public static final ConfigSpec<PasswordPolicyConfig> PASSWORD_POLICY = new SimpleConfigSpec<>(
            new ConfigKey("system.security.password-policy"), "system", "security", "密码策略",
            "系统密码复杂度和强制修改策略", PasswordPolicyConfig.class,
            new PasswordPolicyConfig(8, true, true, false, false, false, false, true), 1, 20,
            ConfigActivationPolicy.DYNAMIC, ConfigEditPolicy.ADMIN_EDITABLE, ConfigInvalidValuePolicy.FAIL_STARTUP,
            List.of(intField("minLength", "最小长度", 10), boolField("requireDigit", "要求数字", 20),
                    boolField("requireLetter", "要求字母", 30), boolField("requireUpper", "要求大写字母", 40),
                    boolField("requireLower", "要求小写字母", 50), boolField("requireSpecial", "要求特殊字符", 60),
                    boolField("forceChangeOnFirstLogin", "首次登录强制修改", 70),
                    boolField("forceChangeOnReset", "重置后强制修改", 80)), "default",
            SystemAuthConfigSpecs::validatePasswordRuntime, SystemAuthConfigSpecs::validatePasswordPublish);

    public record AuthenticationConfig(
            long internalSessionTtlSeconds,
            boolean internalSingleLoginEnabled,
            long sessionCacheTtlSeconds,
            long lastAccessRefreshIntervalSeconds,
            long externalAccessTokenTtlSeconds,
            long externalRefreshTokenTtlSeconds,
            long externalAccessTokenRefreshSkewSeconds,
            String externalJwtIssuer
    ) {
    }

    public record PasswordPolicyConfig(
            int minLength,
            boolean requireDigit,
            boolean requireLetter,
            boolean requireUpper,
            boolean requireLower,
            boolean requireSpecial,
            boolean forceChangeOnFirstLogin,
            boolean forceChangeOnReset
    ) {
    }

    private static List<ConfigViolation> validateAuthentication(AuthenticationConfig value) {
        var violations = new ArrayList<ConfigViolation>();
        positive(value.internalSessionTtlSeconds(), "internalSessionTtlSeconds", violations);
        positive(value.sessionCacheTtlSeconds(), "sessionCacheTtlSeconds", violations);
        positive(value.lastAccessRefreshIntervalSeconds(), "lastAccessRefreshIntervalSeconds", violations);
        positive(value.externalAccessTokenTtlSeconds(), "externalAccessTokenTtlSeconds", violations);
        positive(value.externalRefreshTokenTtlSeconds(), "externalRefreshTokenTtlSeconds", violations);
        if (value.externalAccessTokenRefreshSkewSeconds() < 0 || value.externalAccessTokenRefreshSkewSeconds() >= value.externalAccessTokenTtlSeconds()) {
            violations.add(violation("externalAccessTokenRefreshSkewSeconds", "OUT_OF_RANGE",
                    "令牌提前刷新秒数必须小于访问令牌有效期"));
        }
        if (value.externalRefreshTokenTtlSeconds() <= value.externalAccessTokenTtlSeconds()) {
            violations.add(violation("externalRefreshTokenTtlSeconds", "OUT_OF_RANGE",
                    "刷新令牌有效期必须大于访问令牌有效期"));
        }
        if (value.externalJwtIssuer() == null || value.externalJwtIssuer().isBlank()) {
            violations.add(violation("externalJwtIssuer", "REQUIRED", "JWT 签发者不能为空"));
        }
        return violations;
    }

    private static List<ConfigViolation> validatePasswordRuntime(PasswordPolicyConfig value) {
        return value.minLength() > 0 ? List.of() : List.of(
                violation("minLength", "OUT_OF_RANGE", "密码最小长度必须大于零"));
    }

    private static List<ConfigViolation> validatePasswordPublish(PasswordPolicyConfig value) {
        var violations = new ArrayList<>(validatePasswordRuntime(value));
        if (value.minLength() < 8 || value.minLength() > 128) {
            violations.add(violation("minLength", "OUT_OF_RANGE", "密码最小长度必须在 8 到 128 之间"));
        }
        if ((value.requireUpper() || value.requireLower()) && !value.requireLetter()) {
            violations.add(violation("requireLetter", "DEPENDENCY_REQUIRED", "要求大小写时必须启用字母要求"));
        }
        return violations;
    }

    private static void positive(long value, String path, List<ConfigViolation> violations) {
        if (value <= 0) {
            violations.add(violation(path, "OUT_OF_RANGE", "值必须大于零"));
        }
    }

    private static ConfigFieldSpec longField(String path, String title, int order) {
        return field(path, title, ConfigFieldType.LONG, order);
    }

    private static ConfigFieldSpec intField(String path, String title, int order) {
        return field(path, title, ConfigFieldType.INTEGER, order);
    }

    private static ConfigFieldSpec boolField(String path, String title, int order) {
        return field(path, title, ConfigFieldType.BOOLEAN, order);
    }

    private static ConfigFieldSpec stringField(String path, String title, int order) {
        return field(path, title, ConfigFieldType.STRING, order);
    }

    private static ConfigFieldSpec field(String path, String title, ConfigFieldType type, int order) {
        return new ConfigFieldSpec(path, title, "", type, true, false, false, order, "", null, null, null, null,
                List.of());
    }

    private static ConfigViolation violation(String path, String code, String message) {
        return new ConfigViolation(path, code, message);
    }

    private SystemAuthConfigSpecs() {
    }
}
