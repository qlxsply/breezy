package com.corwin.system.auth.config;

import com.corwin.framework.config.definition.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.corwin.framework.config.definition.ConfigFieldSpecs.required;
import static com.corwin.framework.config.definition.ConfigFieldSpecs.requiredSensitive;

/**
 * @author Corwin 2026/7/31
 */
public final class SystemAuthConfigSpecs {

    public static final ConfigSpec<AuthenticationConfig> AUTHENTICATION = new SimpleConfigSpec<>(
            new ConfigKey("system.security.authentication"), "system", "security", "认证策略",
            "内部会话和外部令牌有效期策略", AuthenticationConfig.class,
            new AuthenticationConfig(28800, false, 300, 60, 1800, 604800, 60,
                    "breezy-external-user-jwt-secret-please-change", "breezy"), 1, 10, ConfigActivationPolicy.DYNAMIC,
            ConfigEditPolicy.ADMIN_EDITABLE, ConfigInvalidValuePolicy.FAIL_STARTUP,
            List.of(required("adminSessionTtlSeconds", "admin会话有效秒数", ConfigFieldType.LONG, 10),
                    required("adminSingleLoginEnabled", "admin账号单点登录", ConfigFieldType.BOOLEAN, 20),
                    required("sessionCacheTtlSeconds", "会话缓存秒数", ConfigFieldType.LONG, 30),
                    required("lastAccessRefreshIntervalSeconds", "访问时间刷新间隔秒数", ConfigFieldType.LONG, 40),
                    required("userAccessTokenTtlSeconds", "访问令牌有效秒数", ConfigFieldType.LONG, 50),
                    required("userRefreshTokenTtlSeconds", "刷新令牌有效秒数", ConfigFieldType.LONG, 60),
                    required("userAccessTokenRefreshSkewSeconds", "令牌提前刷新秒数", ConfigFieldType.LONG, 70),
                    requiredSensitive("userJwtSecret", "用户 JWT 密钥", ConfigFieldType.STRING, 80),
                    required("userJwtIssuer", "JWT 签发者", ConfigFieldType.STRING, 90)), "default",
            SystemAuthConfigSpecs::validateAuthentication, SystemAuthConfigSpecs::validateAuthentication);

    public static final ConfigSpec<PasswordPolicyConfig> PASSWORD_POLICY = new SimpleConfigSpec<>(
            new ConfigKey("system.security.password-policy"), "system", "security", "密码策略",
            "系统密码复杂度和强制修改策略", PasswordPolicyConfig.class,
            new PasswordPolicyConfig(8, true, true, false, false, false, false, true), 1, 20,
            ConfigActivationPolicy.DYNAMIC, ConfigEditPolicy.ADMIN_EDITABLE, ConfigInvalidValuePolicy.FAIL_STARTUP,
            List.of(required("minLength", "最小长度", ConfigFieldType.INTEGER, 10),
                    required("requireDigit", "要求数字", ConfigFieldType.BOOLEAN, 20),
                    required("requireLetter", "要求字母", ConfigFieldType.BOOLEAN, 30),
                    required("requireUpper", "要求大写字母", ConfigFieldType.BOOLEAN, 40),
                    required("requireLower", "要求小写字母", ConfigFieldType.BOOLEAN, 50),
                    required("requireSpecial", "要求特殊字符", ConfigFieldType.BOOLEAN, 60),
                    required("forceChangeOnFirstLogin", "首次登录强制修改", ConfigFieldType.BOOLEAN, 70),
                    required("forceChangeOnReset", "重置后强制修改", ConfigFieldType.BOOLEAN, 80)), "default",
            SystemAuthConfigSpecs::validatePasswordRuntime, SystemAuthConfigSpecs::validatePasswordPublish);

    public record AuthenticationConfig(
            long adminSessionTtlSeconds,
            boolean adminSingleLoginEnabled,
            long sessionCacheTtlSeconds,
            long lastAccessRefreshIntervalSeconds,
            long userAccessTokenTtlSeconds,
            long userRefreshTokenTtlSeconds,
            long userAccessTokenRefreshSkewSeconds,
            String userJwtSecret,
            String userJwtIssuer
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
        positive(value.adminSessionTtlSeconds(), "adminSessionTtlSeconds", violations);
        positive(value.sessionCacheTtlSeconds(), "sessionCacheTtlSeconds", violations);
        positive(value.lastAccessRefreshIntervalSeconds(), "lastAccessRefreshIntervalSeconds", violations);
        positive(value.userAccessTokenTtlSeconds(), "userAccessTokenTtlSeconds", violations);
        positive(value.userRefreshTokenTtlSeconds(), "userRefreshTokenTtlSeconds", violations);
        if (value.userAccessTokenRefreshSkewSeconds() < 0 || value.userAccessTokenRefreshSkewSeconds() >= value.userAccessTokenTtlSeconds()) {
            violations.add(violation("userAccessTokenRefreshSkewSeconds", "OUT_OF_RANGE",
                    "令牌提前刷新秒数必须小于访问令牌有效期"));
        }
        if (value.userRefreshTokenTtlSeconds() <= value.userAccessTokenTtlSeconds()) {
            violations.add(
                    violation("userRefreshTokenTtlSeconds", "OUT_OF_RANGE", "刷新令牌有效期必须大于访问令牌有效期"));
        }
        if (value.userJwtSecret() == null || value.userJwtSecret().getBytes(StandardCharsets.UTF_8).length < 32) {
            violations.add(violation("userJwtSecret", "INVALID_LENGTH", "用户 JWT 密钥不能少于 32 个 UTF-8 字节"));
        }
        if (value.userJwtIssuer() == null || value.userJwtIssuer().isBlank()) {
            violations.add(violation("userJwtIssuer", "REQUIRED", "JWT 签发者不能为空"));
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

    private static ConfigViolation violation(String path, String code, String message) {
        return new ConfigViolation(path, code, message);
    }

    private SystemAuthConfigSpecs() {
    }
}
