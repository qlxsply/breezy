package com.corwin.system.config.application.command;

import java.util.Objects;

/**
 * Command object for updating a configuration value.
 * Encapsulates the config code and the raw value to be set.
 *
 * @author Corwin 2026/1/11
 */
public record UpdateConfigValueCommand(
        String code,
        String rawValue
) {

    public UpdateConfigValueCommand {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(rawValue, "rawValue must not be null");
    }

}
