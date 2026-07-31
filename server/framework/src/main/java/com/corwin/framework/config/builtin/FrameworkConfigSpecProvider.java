package com.corwin.framework.config.builtin;

import com.corwin.framework.config.definition.ConfigSpec;
import com.corwin.framework.config.definition.ConfigSpecProvider;

import java.util.Collection;
import java.util.List;

/**
 * @author Corwin 2026/7/31
 */
public final class FrameworkConfigSpecProvider implements ConfigSpecProvider {

    @Override
    public Collection<ConfigSpec<?>> getConfigSpecs() {
        return List.of(
                FrameworkConfigSpecs.TIME_OFFSET,
                FrameworkConfigSpecs.CLIENT_IP,
                FrameworkConfigSpecs.AUTH_WHITELIST,
                FrameworkConfigSpecs.LOGGING_FILTER
        );
    }
}
