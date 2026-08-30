package com.corwin.framework.config.definition;

import java.util.List;
import java.util.Objects;

/**
 * @author Corwin 2026/7/30
 */
public final class SimpleConfigSpec<T> implements ConfigSpec<T> {

  private final ConfigKey key;
  private final String module;
  private final String group;
  private final String title;
  private final String description;
  private final Class<T> valueClass;
  private final T defaultValue;
  private final int schemaVersion;
  private final int order;
  private final ConfigActivationPolicy activationPolicy;
  private final ConfigEditPolicy editPolicy;
  private final ConfigInvalidValuePolicy invalidValuePolicy;
  private final List<ConfigFieldSpec> fields;
  private final String editorId;
  private final ConfigValidator<T> runtimeValidator;
  private final ConfigValidator<T> publishValidator;

  public SimpleConfigSpec(
      ConfigKey key,
      String module,
      String group,
      String title,
      String description,
      Class<T> valueClass,
      T defaultValue,
      int schemaVersion,
      int order,
      ConfigActivationPolicy activationPolicy,
      ConfigEditPolicy editPolicy,
      ConfigInvalidValuePolicy invalidValuePolicy,
      List<ConfigFieldSpec> fields,
      String editorId,
      ConfigValidator<T> runtimeValidator,
      ConfigValidator<T> publishValidator) {
    this.key = Objects.requireNonNull(key, "key required");
    this.module = requireText(module, "module");
    this.group = requireText(group, "group");
    this.title = requireText(title, "title");
    this.description = description == null ? "" : description;
    this.valueClass = Objects.requireNonNull(valueClass, "valueClass required");
    this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue required");
    if (schemaVersion < 1) {
      throw new IllegalArgumentException("schemaVersion must be greater than zero");
    }
    this.schemaVersion = schemaVersion;
    this.order = order;
    this.activationPolicy = Objects.requireNonNull(activationPolicy, "activationPolicy required");
    this.editPolicy = Objects.requireNonNull(editPolicy, "editPolicy required");
    this.invalidValuePolicy =
        Objects.requireNonNull(invalidValuePolicy, "invalidValuePolicy required");
    this.fields = fields == null ? List.of() : List.copyOf(fields);
    this.editorId = requireText(editorId, "editorId");
    this.runtimeValidator = Objects.requireNonNull(runtimeValidator, "runtimeValidator required");
    this.publishValidator = Objects.requireNonNull(publishValidator, "publishValidator required");
  }

  @Override
  public ConfigKey key() {
    return key;
  }

  @Override
  public String module() {
    return module;
  }

  @Override
  public String group() {
    return group;
  }

  @Override
  public String title() {
    return title;
  }

  @Override
  public String description() {
    return description;
  }

  @Override
  public Class<T> valueClass() {
    return valueClass;
  }

  @Override
  public T defaultValue() {
    return defaultValue;
  }

  @Override
  public int schemaVersion() {
    return schemaVersion;
  }

  @Override
  public int order() {
    return order;
  }

  @Override
  public ConfigActivationPolicy activationPolicy() {
    return activationPolicy;
  }

  @Override
  public ConfigEditPolicy editPolicy() {
    return editPolicy;
  }

  @Override
  public ConfigInvalidValuePolicy invalidValuePolicy() {
    return invalidValuePolicy;
  }

  @Override
  public List<ConfigFieldSpec> fields() {
    return fields;
  }

  @Override
  public String editorId() {
    return editorId;
  }

  @Override
  public List<ConfigViolation> validateRuntime(T value) {
    return immutableViolations(runtimeValidator.validate(value));
  }

  @Override
  public List<ConfigViolation> validatePublish(T value) {
    return immutableViolations(publishValidator.validate(value));
  }

  private static List<ConfigViolation> immutableViolations(List<ConfigViolation> violations) {
    return violations == null ? List.of() : List.copyOf(violations);
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
