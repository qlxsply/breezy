package com.corwin.framework.config.runtime;

import com.corwin.framework.config.definition.ConfigSpec;

/**
 * @author Corwin 2026/7/30
 */
public final class Configs {

    public static <T> T get(ConfigSpec<T> spec) {
        return ConfigRegistry.get(spec);
    }

    public static <T> ConfigSnapshot<T> snapshot(ConfigSpec<T> spec) {
        return ConfigRegistry.snapshot(spec);
    }

    private Configs() {
    }
}
