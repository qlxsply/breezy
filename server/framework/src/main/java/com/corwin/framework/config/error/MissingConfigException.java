package com.corwin.framework.config.error;

/**
 * @author Corwin 2026/7/30
 */
public class MissingConfigException extends RuntimeException {

  public MissingConfigException(String key) {
    super("Missing config snapshot: " + key);
  }
}
