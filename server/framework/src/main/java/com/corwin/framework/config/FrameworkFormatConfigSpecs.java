package com.corwin.framework.config;

import com.corwin.framework.config.definition.*;

import java.math.BigDecimal;
import java.util.List;

import static com.corwin.framework.config.definition.ConfigFieldSpecs.requiredEnum;
import static com.corwin.framework.config.definition.ConfigFieldSpecs.requiredRange;

/**
 * framework 通用数字格式化配置。
 *
 * @author Corwin 2026/7/31
 */
public final class FrameworkFormatConfigSpecs {

    public static final ConfigSpec<DecimalPolicyConfig> DECIMAL_POLICY = new SimpleConfigSpec<>(
            new ConfigKey("framework.format.decimal-policy"), "framework", "format", "通用数字策略",
            "系统通用数字展示的小数位数与舍入模式", DecimalPolicyConfig.class,
            new DecimalPolicyConfig(2, DecimalRoundingMode.HALF_UP), 1, 10, ConfigActivationPolicy.DYNAMIC,
            ConfigEditPolicy.ADMIN_EDITABLE, ConfigInvalidValuePolicy.USE_DEFAULT,
            List.of(requiredRange("scale", "小数位数", ConfigFieldType.INTEGER, 10, BigDecimal.ZERO,
                            BigDecimal.valueOf(20)),
                    requiredEnum("roundingMode", "舍入模式", DecimalRoundingMode.values(), 20)), "decimal-policy",
            FrameworkFormatConfigSpecs::validateDecimalPolicy, FrameworkFormatConfigSpecs::validateDecimalPolicy);

    public record DecimalPolicyConfig(
            int scale,
            DecimalRoundingMode roundingMode
    ) {
    }

    private static List<ConfigViolation> validateDecimalPolicy(DecimalPolicyConfig value) {
        if (value == null || value.roundingMode() == null) {
            return List.of(new ConfigViolation("roundingMode", "REQUIRED", "舍入模式不能为空"));
        }
        if (value.scale() < 0 || value.scale() > 20) {
            return List.of(new ConfigViolation("scale", "OUT_OF_RANGE", "小数位数必须在 0 到 20 之间"));
        }
        return List.of();
    }

    private FrameworkFormatConfigSpecs() {
    }
}
