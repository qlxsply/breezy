package com.corwin.schemaforge.config;

import com.corwin.framework.config.definition.ConfigSpec;
import com.corwin.framework.config.definition.ConfigSpecProvider;

import java.util.Collection;
import java.util.List;

/**
 * @author Corwin 2026/7/31
 */
public final class SchemaForgeConfigSpecProvider implements ConfigSpecProvider {

    @Override
    public Collection<ConfigSpec<?>> getConfigSpecs() {
        return List.of(SchemaForgeConfigSpecs.DDL_POLICY);
    }
}
