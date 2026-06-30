package com.corwin.bootstrap.application.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * @author Corwin 2026/4/23
 */
@Component
public class BootstrapDefinitionResources {

    private static final String RESOURCES_XML = "bootstarp/resources.xml";
    private static final String RESOURCES_XSD = "bootstarp/resources.xsd";
    private static final String USER_FEATURES_XML = "bootstarp/user-features.xml";
    private static final String USER_FEATURES_XSD = "bootstarp/user-features.xsd";
    private static final String DICTS_XML = "bootstarp/dictionaries.xml";
    private static final String DICTS_XSD = "bootstarp/dictionaries.xsd";
    private static final String PERMISSIONS_PROPERTIES = "bootstarp/permissions.properties";

    public Resource resourcesXml() {
        return new ClassPathResource(RESOURCES_XML);
    }

    public Resource resourcesXsd() {
        return new ClassPathResource(RESOURCES_XSD);
    }

    public Resource userFeaturesXml() {
        return new ClassPathResource(USER_FEATURES_XML);
    }

    public Resource userFeaturesXsd() {
        return new ClassPathResource(USER_FEATURES_XSD);
    }

    public Resource dictionariesXml() {
        return new ClassPathResource(DICTS_XML);
    }

    public Resource dictionariesXsd() {
        return new ClassPathResource(DICTS_XSD);
    }

    public Resource permissionsProperties() {
        return new ClassPathResource(PERMISSIONS_PROPERTIES);
    }
}
