package com.corwin.schemaforge.config;

import static com.corwin.framework.config.definition.ConfigFieldSpecs.required;
import static com.corwin.framework.config.definition.ConfigFieldSpecs.requiredEnum;

import com.corwin.framework.config.definition.*;
import java.util.List;

/**
 * @author Corwin 2026/7/31
 */
public final class SchemaForgeConfigSpecs {

  public static final ConfigSpec<DdlPolicy> DDL_POLICY =
      new SimpleConfigSpec<>(
          new ConfigKey("schemaforge.ddl.policy"),
          "schemaforge",
          "ddl",
          "DDL 生成策略",
          "SchemaForge DDL 格式化与限定符生成策略",
          DdlPolicy.class,
          new DdlPolicy(true, QualifierMode.ALWAYS_SOURCE),
          1,
          10,
          ConfigActivationPolicy.DYNAMIC,
          ConfigEditPolicy.ADMIN_EDITABLE,
          ConfigInvalidValuePolicy.USE_DEFAULT,
          List.of(
              required("formatEnabled", "启用 DDL 格式化", ConfigFieldType.BOOLEAN, 10),
              requiredEnum("qualifierMode", "限定符模式", QualifierMode.values(), 20)),
          "default",
          SchemaForgeConfigSpecs::validateDdlPolicy,
          SchemaForgeConfigSpecs::validateDdlPolicy);

  public record DdlPolicy(boolean formatEnabled, QualifierMode qualifierMode) {}

  public enum QualifierMode {
    AUTO,
    ALWAYS_SOURCE,
    NEVER
  }

  private static List<ConfigViolation> validateDdlPolicy(DdlPolicy value) {
    return value != null && value.qualifierMode() != null
        ? List.of()
        : List.of(new ConfigViolation("qualifierMode", "REQUIRED", "限定符模式不能为空"));
  }

  private SchemaForgeConfigSpecs() {}
}
