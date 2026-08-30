package com.corwin.framework.config.error;

import com.corwin.framework.config.definition.ConfigViolation;
import java.util.List;

/**
 * @author Corwin 2026/7/30
 */
public class ConfigValidationException extends RuntimeException {

  private final List<ConfigViolation> violations;

  public ConfigValidationException(String message, List<ConfigViolation> violations) {
    super(message);
    this.violations = violations == null ? List.of() : List.copyOf(violations);
  }

  public List<ConfigViolation> violations() {
    return violations;
  }
}
