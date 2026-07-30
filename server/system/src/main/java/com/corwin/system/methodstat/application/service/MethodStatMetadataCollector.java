package com.corwin.system.methodstat.application.service;

/**
 * Interface for collecting metadata for all methods matching the configured pointcuts.
 * @author Corwin 2026/3/31
 */
public interface MethodStatMetadataCollector {

    /**
     * Collect metadata for all pointcut-matched beans in the application context.
     * @return number of methods registered
     */
    int collectAllPointcutMetadata();

}
