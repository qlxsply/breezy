package com.corwin.system.config.application.command;

import java.util.Objects;

/**
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
