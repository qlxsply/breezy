package com.corwin.framework.config;

/**
 * Contract for a single config definition: its unique name, description, value type, and level.
 *
 * @author Corwin 2026/1/31
 */
public interface ConfigDefinition {

    String name();

    String desc();

    ConfigValueType valueType();

    ConfigLevel level();

}
