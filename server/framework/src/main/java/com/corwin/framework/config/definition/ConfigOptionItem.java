package com.corwin.framework.config.definition;

/**
 * @author Corwin 2026/7/30
 */
public record ConfigOptionItem(String value, String label) {

  public ConfigOptionItem {
    requireText(value, "value");
    requireText(label, "label");
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}
