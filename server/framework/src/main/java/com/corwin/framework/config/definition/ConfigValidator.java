package com.corwin.framework.config.definition;

import java.util.List;

/**
 * @author Corwin 2026/7/30
 */
@FunctionalInterface
public interface ConfigValidator<T> {

    List<ConfigViolation> validate(T value);
}
