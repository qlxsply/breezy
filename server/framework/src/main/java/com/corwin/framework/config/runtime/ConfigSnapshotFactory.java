package com.corwin.framework.config.runtime;

import com.corwin.framework.config.codec.ConfigJsonCodec;
import com.corwin.framework.config.definition.ConfigInvalidValuePolicy;
import com.corwin.framework.config.definition.ConfigSpec;
import com.corwin.framework.config.definition.ConfigViolation;
import com.corwin.framework.config.error.ConfigLoadException;
import com.corwin.framework.config.error.ConfigValidationException;
import com.corwin.framework.util.HighDate;
import java.util.List;

/**
 * @author Corwin 2026/7/30
 */
public final class ConfigSnapshotFactory {

  private static final int WARNING_MAX_LENGTH = 500;

  public static <T> ConfigSnapshot<T> createDefault(ConfigSpec<T> spec) {
    T value = validatedDefault(spec);
    return snapshot(spec, value, 0, ConfigValueSource.CODE_DEFAULT, "");
  }

  public static <T> ConfigSnapshot<T> createStored(
      ConfigSpec<T> spec, String content, int schemaVersion, long revision, boolean configured) {
    if (!configured) {
      return snapshot(spec, validatedDefault(spec), revision, ConfigValueSource.CODE_DEFAULT, "");
    }
    try {
      if (schemaVersion != spec.schemaVersion()) {
        throw new ConfigLoadException(
            "Schema version mismatch: stored="
                + schemaVersion
                + ", expected="
                + spec.schemaVersion());
      }
      T value = ConfigJsonCodec.deserialize(content, spec.valueClass());
      validateRuntime(spec, value);
      return snapshot(spec, value, revision, ConfigValueSource.DATABASE_OVERRIDE, "");
    } catch (RuntimeException ex) {
      if (spec.invalidValuePolicy() == ConfigInvalidValuePolicy.FAIL_STARTUP) {
        throw new ConfigLoadException("Failed to load config " + spec.key(), ex);
      }
      return snapshot(
          spec,
          validatedDefault(spec),
          revision,
          ConfigValueSource.INVALID_DATABASE_FALLBACK,
          warning(ex));
    }
  }

  private static <T> T validatedDefault(ConfigSpec<T> spec) {
    T value = spec.valueClass().cast(spec.defaultValue());
    validateRuntime(spec, value);
    return value;
  }

  private static <T> void validateRuntime(ConfigSpec<T> spec, T value) {
    List<ConfigViolation> violations = spec.validateRuntime(value);
    if (violations != null && !violations.isEmpty()) {
      throw new ConfigValidationException(
          "Config runtime validation failed: " + spec.key(), violations);
    }
  }

  private static <T> ConfigSnapshot<T> snapshot(
      ConfigSpec<T> spec, T value, long revision, ConfigValueSource source, String warning) {
    return new ConfigSnapshot<>(
        spec.key(), value, revision, spec.schemaVersion(), source, HighDate.realInstant(), warning);
  }

  private static String warning(RuntimeException ex) {
    String message = ex.getMessage();
    String warning =
        ex.getClass().getSimpleName()
            + (message == null || message.isBlank() ? "" : ": " + message);
    return warning.length() <= WARNING_MAX_LENGTH
        ? warning
        : warning.substring(0, WARNING_MAX_LENGTH);
  }

  private ConfigSnapshotFactory() {}
}
