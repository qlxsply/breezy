package com.corwin.framework.config;

/**
 * @author Corwin 2026/1/31
 */
public interface ConfigDefinition {

    String name();

    String desc();

    ConfigValueType valueType();

    ConfigLevel level();

}
