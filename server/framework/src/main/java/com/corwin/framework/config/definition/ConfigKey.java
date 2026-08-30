package com.corwin.framework.config.definition;

import java.util.regex.Pattern;

/**
 * @author Corwin 2026/7/30
 */
public record ConfigKey(String value) {

  public static final int MAX_LENGTH = 160;

  private static final Pattern PATTERN = Pattern.compile("[a-z][a-z0-9]*(\\.[a-z][a-z0-9-]*)+");

  public ConfigKey {
    if (value == null || value.length() > MAX_LENGTH || !PATTERN.matcher(value).matches()) {
      throw new IllegalArgumentException("Invalid config key: " + value);
    }
  }

  @Override
  public String toString() {
    return value;
  }
}
