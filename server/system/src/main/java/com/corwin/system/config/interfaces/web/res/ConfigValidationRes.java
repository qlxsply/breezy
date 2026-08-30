package com.corwin.system.config.interfaces.web.res;

import com.corwin.framework.config.definition.ConfigViolation;
import java.util.List;

/**
 * @author Corwin 2026/7/31
 */
public record ConfigValidationRes(boolean valid, List<ConfigViolation> violations) {

  public ConfigValidationRes {
    violations = List.copyOf(violations);
  }
}
