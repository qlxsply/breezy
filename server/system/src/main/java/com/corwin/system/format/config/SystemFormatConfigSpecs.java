package com.corwin.system.format.config;

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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * 系统通用数字格式化配置。
 *
 * @author Corwin 2026/7/31
 */
public final class SystemFormatConfigSpecs {

    public static final ConfigSpec<DecimalPolicyConfig> DECIMAL_POLICY = new SimpleConfigSpec<>(
            new ConfigKey("system.format.decimal-policy"), "system", "format", "通用数字策略",
            "系统通用数字展示的小数位数与舍入模式", DecimalPolicyConfig.class,
            new DecimalPolicyConfig(2, DecimalRoundingMode.HALF_UP), 1, 10,
            ConfigActivationPolicy.DYNAMIC, ConfigEditPolicy.ADMIN_EDITABLE, ConfigInvalidValuePolicy.USE_DEFAULT,
            List.of(
                    field("scale", "小数位数", ConfigFieldType.INTEGER, 10, 0, 20),
                    enumField("roundingMode", "舍入模式", DecimalRoundingMode.values(), 20)
            ), "default", SystemFormatConfigSpecs::validateDecimalPolicy,
            SystemFormatConfigSpecs::validateDecimalPolicy
    );

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

    private static ConfigFieldSpec field(
            String path,
            String title,
            ConfigFieldType type,
            int order,
            int min,
            int max
    ) {
        return new ConfigFieldSpec(path, title, "", type, true, false, false, order, "",
                BigDecimal.valueOf(min), BigDecimal.valueOf(max), null, null, List.of());
    }

    private static ConfigFieldSpec enumField(String path, String title, Enum<?>[] values, int order) {
        return new ConfigFieldSpec(path, title, "", ConfigFieldType.ENUM, true, false, false, order, "",
                null, null, null, null,
                Arrays.stream(values)
                        .map(value -> new ConfigOptionItem(value.name(), value.name()))
                        .toList());
    }

    private SystemFormatConfigSpecs() {
    }
}
