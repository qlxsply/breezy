package com.corwin.system.config.application.view;

import com.corwin.framework.config.definition.ConfigViolation;

import java.util.List;

/**
 * @author Corwin 2026/7/30
 */
public record ConfigValidationView(
        boolean valid,
        List<ConfigViolation> violations
) {

    public ConfigValidationView {
        violations = List.copyOf(violations);
    }
}
