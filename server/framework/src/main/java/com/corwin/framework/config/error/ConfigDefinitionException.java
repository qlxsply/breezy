package com.corwin.framework.config.error;

/**
 * @author Corwin 2026/7/30
 */
public class ConfigDefinitionException extends RuntimeException {

    public ConfigDefinitionException(String message) {
        super(message);
    }

    public ConfigDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
