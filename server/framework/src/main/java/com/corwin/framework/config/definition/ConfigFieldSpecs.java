package com.corwin.framework.config.definition;

import com.corwin.framework.dict.DictEnumDefinition;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * 配置字段定义的通用构造方法。
 *
 * @author Corwin 2026/8/3
 */
public final class ConfigFieldSpecs {

  public static ConfigFieldSpec required(
      String path, String title, ConfigFieldType type, int order) {
    return field(path, title, type, true, false, order, null, null, List.of());
  }

  public static ConfigFieldSpec requiredSensitive(
      String path, String title, ConfigFieldType type, int order) {
    return field(path, title, type, true, true, order, null, null, List.of());
  }

  public static ConfigFieldSpec optional(
      String path, String title, ConfigFieldType type, int order) {
    return field(path, title, type, false, false, order, null, null, List.of());
  }

  public static ConfigFieldSpec optionalSensitive(
      String path, String title, ConfigFieldType type, int order) {
    return field(path, title, type, false, true, order, null, null, List.of());
  }

  public static ConfigFieldSpec requiredRange(
      String path, String title, ConfigFieldType type, int order, BigDecimal min, BigDecimal max) {
    return field(path, title, type, true, false, order, min, max, List.of());
  }

  public static <E extends Enum<E>> ConfigFieldSpec requiredEnum(
      String path, String title, E[] values, int order) {
    List<ConfigOptionItem> options =
        Arrays.stream(values)
            .map(
                value ->
                    new ConfigOptionItem(
                        value instanceof DictEnumDefinition definition
                            ? definition.itemValue()
                            : value.name(),
                        value instanceof DictEnumDefinition definition
                            ? definition.label()
                            : value.name()))
            .toList();
    return field(path, title, ConfigFieldType.ENUM, true, false, order, null, null, options);
  }

  private static ConfigFieldSpec field(
      String path,
      String title,
      ConfigFieldType type,
      boolean required,
      boolean sensitive,
      int order,
      BigDecimal min,
      BigDecimal max,
      List<ConfigOptionItem> options) {
    return new ConfigFieldSpec(
        path, title, "", type, required, sensitive, false, order, "", min, max, null, null,
        options);
  }

  private ConfigFieldSpecs() {}
}
