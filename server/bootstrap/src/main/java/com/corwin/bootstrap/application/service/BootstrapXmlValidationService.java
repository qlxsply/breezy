package com.corwin.bootstrap.application.service;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

/**
 * @author Corwin 2026/4/28
 */
@Component
public class BootstrapXmlValidationService {

    public void validate(Resource xmlResource, Resource xsdResource) {
        try (var xsdInput = xsdResource.getInputStream();
                var xmlInput = xmlResource.getInputStream()) {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(new StreamSource(xsdInput));
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(xmlInput));
        } catch (Exception e) {
            throw new IllegalStateException("XML validation failed: " + xmlResource.getFilename(), e);
        }
    }
}
