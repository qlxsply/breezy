package com.corwin.framework.config.bootstrap;

/**
 * @author Corwin 2026/7/30
 */
public record RawConfigValue(
    String configKey, String content, int schemaVersion, long revision, boolean configured) {

  public RawConfigValue {
    if (configKey == null || configKey.isBlank()) {
      throw new IllegalArgumentException("configKey must not be blank");
    }
    if (content == null) {
      throw new IllegalArgumentException("content must not be null");
    }
    if (schemaVersion < 1) {
      throw new IllegalArgumentException("schemaVersion must be greater than zero");
    }
    if (revision < 1) {
      throw new IllegalArgumentException("revision must be greater than zero");
    }
  }
}
