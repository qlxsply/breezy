package com.corwin.framework.config.definition;

import java.util.List;

/**
 * @author Corwin 2026/7/30
 */
public interface ConfigSpec<T> {

    ConfigKey key();

    String module();

    String group();

    String title();

    String description();

    Class<T> valueClass();

    T defaultValue();

    int schemaVersion();

    int order();

    ConfigActivationPolicy activationPolicy();

    ConfigEditPolicy editPolicy();

    ConfigInvalidValuePolicy invalidValuePolicy();

    List<ConfigFieldSpec> fields();

    default String editorId() {
        return "default";
    }

    List<ConfigViolation> validateRuntime(T value);

    List<ConfigViolation> validatePublish(T value);
}
