package com.corwin.schemaforge.config;

import com.corwin.framework.config.definition.*;

import java.util.Arrays;
import java.util.List;

/**
 * @author Corwin 2026/7/31
 */
public final class SchemaForgeConfigSpecs {

    public static final ConfigSpec<DdlPolicy> DDL_POLICY = new SimpleConfigSpec<>(
            new ConfigKey("schemaforge.ddl.policy"), "schemaforge", "ddl", "DDL 生成策略",
            "SchemaForge DDL 格式化与限定符生成策略", DdlPolicy.class, new DdlPolicy(true, QualifierMode.ALWAYS_SOURCE),
            1, 10, ConfigActivationPolicy.DYNAMIC, ConfigEditPolicy.ADMIN_EDITABLE,
            ConfigInvalidValuePolicy.USE_DEFAULT,
            List.of(field("formatEnabled", "启用 DDL 格式化", ConfigFieldType.BOOLEAN, 10),
                    enumField("qualifierMode", "限定符模式", QualifierMode.values(), 20)), "default",
            SchemaForgeConfigSpecs::validateDdlPolicy, SchemaForgeConfigSpecs::validateDdlPolicy);

    public record DdlPolicy(
            boolean formatEnabled,
            QualifierMode qualifierMode
    ) {
    }

    public enum QualifierMode {
        AUTO,
        ALWAYS_SOURCE,
        NEVER
    }

    private static List<ConfigViolation> validateDdlPolicy(DdlPolicy value) {
        return value != null && value.qualifierMode() != null ? List.of() : List.of(
                new ConfigViolation("qualifierMode", "REQUIRED", "限定符模式不能为空"));
    }

    private static ConfigFieldSpec field(String path, String title, ConfigFieldType type, int order) {
        return new ConfigFieldSpec(path, title, "", type, true, false, false, order, "", null, null, null, null,
                List.of());
    }

    private static ConfigFieldSpec enumField(String path, String title, Enum<?>[] values, int order) {
        return new ConfigFieldSpec(path, title, "", ConfigFieldType.ENUM, true, false, false, order, "", null, null,
                null, null,
                Arrays.stream(values).map(value -> new ConfigOptionItem(value.name(), value.name())).toList());
    }

    private SchemaForgeConfigSpecs() {
    }
}
