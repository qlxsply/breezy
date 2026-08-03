package com.corwin.system.audit.config;

import com.corwin.framework.config.definition.*;

import java.util.ArrayList;
import java.util.List;

import static com.corwin.framework.config.definition.ConfigFieldSpecs.required;

/**
 * @author Corwin 2026/7/31
 */
public final class SystemAuditConfigSpecs {

    public static final ConfigSpec<AuditPolicyConfig> AUDIT_POLICY = new SimpleConfigSpec<>(
            new ConfigKey("system.audit.policy"), "system", "audit", "审计策略", "审计开关、异步记录和摘要长度",
            AuditPolicyConfig.class, new AuditPolicyConfig(true, true, 4096, 4096), 1, 10,
            ConfigActivationPolicy.DYNAMIC, ConfigEditPolicy.ADMIN_EDITABLE, ConfigInvalidValuePolicy.USE_DEFAULT,
            List.of(required("enabled", "启用审计", ConfigFieldType.BOOLEAN, 10),
                    required("asyncEnabled", "异步记录", ConfigFieldType.BOOLEAN, 20),
                    required("requestSummaryMaxLength", "请求摘要最大长度", ConfigFieldType.INTEGER, 30),
                    required("responseSummaryMaxLength", "响应摘要最大长度", ConfigFieldType.INTEGER, 40)), "default",
            SystemAuditConfigSpecs::validate, SystemAuditConfigSpecs::validate);

    public record AuditPolicyConfig(
            boolean enabled,
            boolean asyncEnabled,
            int requestSummaryMaxLength,
            int responseSummaryMaxLength
    ) {
    }

    private static List<ConfigViolation> validate(AuditPolicyConfig value) {
        var violations = new ArrayList<ConfigViolation>();
        if (value.requestSummaryMaxLength() <= 0) {
            violations.add(new ConfigViolation("requestSummaryMaxLength", "OUT_OF_RANGE", "请求摘要长度必须大于零"));
        }
        if (value.responseSummaryMaxLength() <= 0) {
            violations.add(new ConfigViolation("responseSummaryMaxLength", "OUT_OF_RANGE", "响应摘要长度必须大于零"));
        }
        return violations;
    }

    private SystemAuditConfigSpecs() {
    }
}
