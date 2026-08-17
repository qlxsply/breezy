package com.corwin.framework.config.builtin;

import com.corwin.framework.config.TimeMockMode;
import com.corwin.framework.config.definition.ConfigActivationPolicy;
import com.corwin.framework.config.definition.ConfigEditPolicy;
import com.corwin.framework.config.definition.ConfigFieldSpec;
import com.corwin.framework.config.definition.ConfigFieldType;
import com.corwin.framework.config.definition.ConfigInvalidValuePolicy;
import com.corwin.framework.config.definition.ConfigKey;
import com.corwin.framework.config.definition.ConfigOptionItem;
import com.corwin.framework.config.definition.ConfigSpec;
import com.corwin.framework.config.definition.ConfigViolation;
import com.corwin.framework.config.definition.SimpleConfigSpec;
import com.corwin.framework.util.ClientIpMode;
import com.corwin.framework.web.auth.AuthWhitelistItem;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.corwin.framework.config.definition.ConfigFieldSpecs.required;
import static com.corwin.framework.config.definition.ConfigFieldSpecs.requiredEnum;

/**
 * @author Corwin 2026/7/31
 */
public final class FrameworkConfigSpecs {

    public static final ConfigSpec<TimeMockConfig> TIME_MOCK = new SimpleConfigSpec<>(
            new ConfigKey("framework.time.mock"), "framework", "time", "业务时间模拟",
            "按动态偏移或固定时刻模拟业务时间，不影响调度、安全过期与运行耗时", TimeMockConfig.class,
            new TimeMockConfig(TimeMockMode.DYNAMIC, 0, 0), 1, 10,
            ConfigActivationPolicy.DYNAMIC, ConfigEditPolicy.ADMIN_EDITABLE, ConfigInvalidValuePolicy.USE_DEFAULT,
            List.of(requiredEnum("mode", "模拟模式", TimeMockMode.values(), 10),
                    required("offsetSeconds", "时间偏移秒数", ConfigFieldType.LONG, 20),
                    required("fixedEpochMillis", "固定时间", ConfigFieldType.LONG, 30)), "time-offset",
            FrameworkConfigSpecs::validateTimeMock, FrameworkConfigSpecs::validateTimeMock);

    public static final ConfigSpec<ClientIpConfig> CLIENT_IP = new SimpleConfigSpec<>(
            new ConfigKey("framework.web.client-ip"), "framework", "web", "客户端 IP", "客户端 IP 请求头解析方式",
            ClientIpConfig.class, new ClientIpConfig(ClientIpMode.REMOTE_ADDR), 1, 10, ConfigActivationPolicy.DYNAMIC,
            ConfigEditPolicy.ADMIN_EDITABLE, ConfigInvalidValuePolicy.FAIL_STARTUP,
            List.of(new ConfigFieldSpec("mode", "解析模式", "客户端 IP 解析模式", ConfigFieldType.ENUM, true, false,
                    false, 10, "", null, null, null, null, List.of(ClientIpMode.values()).stream()
                    .map(value -> new ConfigOptionItem(value.name(), value.name())).toList())), "default",
            FrameworkConfigSpecs::validateClientIp, FrameworkConfigSpecs::validateClientIp);

    public static final ConfigSpec<AuthWhitelistConfig> AUTH_WHITELIST = new SimpleConfigSpec<>(
            new ConfigKey("framework.web.auth-whitelist"), "framework", "web", "鉴权白名单",
            "无需登录即可访问的路径规则", AuthWhitelistConfig.class, new AuthWhitelistConfig(
            List.of(new AuthWhitelistItem("EXACT", "/api/auth/login"),
                    new AuthWhitelistItem("EXACT", "/api/auth/logout"), new AuthWhitelistItem("EXACT", "/api/auth/me"),
                    new AuthWhitelistItem("ANT", "/api/public/frontend-resources/**"),
                    new AuthWhitelistItem("EXACT", "/h2-console"), new AuthWhitelistItem("ANT", "/h2-console/**"))), 1,
            20, ConfigActivationPolicy.DYNAMIC, ConfigEditPolicy.ADMIN_EDITABLE, ConfigInvalidValuePolicy.FAIL_STARTUP,
            List.of(required("rules", "白名单规则", ConfigFieldType.OBJECT, 10)), "auth-whitelist",
            FrameworkConfigSpecs::validateWhitelist, FrameworkConfigSpecs::validateWhitelist);

    public static final ConfigSpec<LoggingFilterConfig> LOGGING_FILTER = new SimpleConfigSpec<>(
            new ConfigKey("framework.web.logging-filter"), "framework", "web", "请求日志过滤",
            "请求日志排除路径和流式响应路径", LoggingFilterConfig.class,
            new LoggingFilterConfig(List.of("/static/", "/actuator", "/favicon.ico"), List.of("/api/sse/")), 1, 30,
            ConfigActivationPolicy.DYNAMIC, ConfigEditPolicy.ADMIN_EDITABLE, ConfigInvalidValuePolicy.USE_DEFAULT,
            List.of(required("excludePrefixes", "日志排除路径", ConfigFieldType.STRING_LIST, 10),
                    required("streamPrefixes", "流式响应路径", ConfigFieldType.STRING_LIST, 20)), "logging-filter",
            FrameworkConfigSpecs::validateLogging, FrameworkConfigSpecs::validateLogging);

    public record TimeMockConfig(TimeMockMode mode, long offsetSeconds, long fixedEpochMillis) {
    }

    public record ClientIpConfig(ClientIpMode mode) {
    }

    public record AuthWhitelistConfig(List<AuthWhitelistItem> rules) {
        public AuthWhitelistConfig {
            rules = rules == null ? List.of() : List.copyOf(rules);
        }
    }

    public record LoggingFilterConfig(
            List<String> excludePrefixes,
            List<String> streamPrefixes
    ) {
        public LoggingFilterConfig {
            excludePrefixes = excludePrefixes == null ? List.of() : List.copyOf(excludePrefixes);
            streamPrefixes = streamPrefixes == null ? List.of() : List.copyOf(streamPrefixes);
        }
    }

    private static List<ConfigViolation> validateTimeMock(TimeMockConfig value) {
        if (value == null || value.mode() == null) {
            return List.of(violation("mode", "REQUIRED", "时间模拟模式不能为空"));
        }
        try {
            if (value.mode() == TimeMockMode.DYNAMIC) {
                Instant.now().plusSeconds(value.offsetSeconds());
            } else {
                Instant.ofEpochMilli(value.fixedEpochMillis());
            }
            return List.of();
        } catch (DateTimeException ex) {
            String path = value.mode() == TimeMockMode.DYNAMIC ? "offsetSeconds" : "fixedEpochMillis";
            return List.of(violation(path, "OUT_OF_RANGE", "模拟时间超出可计算范围"));
        }
    }

    private static List<ConfigViolation> validateClientIp(ClientIpConfig value) {
        return value != null && value.mode() != null ? List.of() : List.of(
                violation("mode", "REQUIRED", "客户端 IP 解析模式不能为空"));
    }

    private static List<ConfigViolation> validateWhitelist(AuthWhitelistConfig value) {
        if (value == null || value.rules() == null) {
            return List.of(violation("rules", "REQUIRED", "白名单规则不能为空"));
        }
        Set<String> supportedTypes = Set.of("EXACT", "ANT", "PATH_PATTERN");
        for (int index = 0; index < value.rules().size(); index++) {
            AuthWhitelistItem item = value.rules().get(index);
            if (item == null || item.type() == null || !supportedTypes.contains(
                    item.type()) || item.pattern() == null || item.pattern().isBlank() || !item.pattern()
                    .startsWith("/")) {
                return List.of(violation("rules[" + index + "]", "INVALID_RULE", "鉴权白名单规则无效"));
            }
        }
        return List.of();
    }

    private static List<ConfigViolation> validateLogging(LoggingFilterConfig value) {
        if (value == null) {
            return List.of(violation("", "REQUIRED", "日志过滤配置不能为空"));
        }
        var violations = new java.util.ArrayList<ConfigViolation>();
        validatePrefixes("excludePrefixes", value.excludePrefixes(), violations);
        validatePrefixes("streamPrefixes", value.streamPrefixes(), violations);
        return violations;
    }

    private static void validatePrefixes(String path, List<String> values, List<ConfigViolation> violations) {
        if (values == null || values.isEmpty()) {
            violations.add(violation(path, "REQUIRED", "路径前缀不能为空"));
            return;
        }
        var unique = new HashSet<String>();
        for (String value : values) {
            if (value == null || value.isBlank() || !value.startsWith("/") || !unique.add(value)) {
                violations.add(violation(path, "INVALID_PREFIX", "路径前缀必须以 / 开头且不能重复"));
                return;
            }
        }
    }

    private static ConfigViolation violation(String path, String code, String message) {
        return new ConfigViolation(path, code, message);
    }

    private FrameworkConfigSpecs() {
    }
}
