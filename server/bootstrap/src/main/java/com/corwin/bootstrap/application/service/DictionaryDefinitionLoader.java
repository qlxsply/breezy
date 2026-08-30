package com.corwin.bootstrap.application.service;

import com.corwin.framework.dict.DictEnumDefinition;
import com.corwin.framework.util.StrUtil;
import com.corwin.system.dict.domain.model.DictStructureType;
import com.corwin.system.dict.domain.model.DictValueType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.InputStream;
import java.util.*;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import lombok.Getter;
import lombok.Setter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * @author Corwin 2026/4/16
 */
@Component
public class DictionaryDefinitionLoader {

  private static final String DEFAULT_XML = "bootstarp/dictionaries.xml";
  private static final String DEFAULT_XSD = "bootstarp/dictionaries.xsd";

  public List<DictTypeSeed> loadDefaultDefinitions() {
    return loadDefinitions(new ClassPathResource(DEFAULT_XML), new ClassPathResource(DEFAULT_XSD));
  }

  public List<DictTypeSeed> loadDefinitions(Resource xmlResource, Resource xsdResource) {
    try {
      validateXml(xmlResource, xsdResource);

      XmlMapper mapper = new XmlMapper();
      try (InputStream is = xmlResource.getInputStream()) {
        DictionaryRoot root = mapper.readValue(is, DictionaryRoot.class);
        List<DictionaryNode> nodes = root.getDictionaries();
        if (nodes == null || nodes.isEmpty()) {
          return Collections.emptyList();
        }

        checkDictUniqueness(nodes);
        List<DictTypeSeed> result = new ArrayList<>();
        for (DictionaryNode node : nodes) {
          result.add(
              new DictTypeSeed(
                  node.getId(),
                  node.getCode(),
                  node.getName(),
                  node.getDescription(),
                  node.getEnumClass(),
                  node.getValueType(),
                  node.getStructureType(),
                  Boolean.TRUE.equals(node.getEnabled()),
                  flattenEnumDictItems(node)));
        }
        return result;
      }
    } catch (Exception e) {
      throw new IllegalStateException("Dictionary definition load failed: " + e.getMessage(), e);
    }
  }

  private void checkDictUniqueness(List<DictionaryNode> nodes) {
    Set<String> dictCodes = new HashSet<>();
    Set<String> dictIds = new HashSet<>();
    for (DictionaryNode node : nodes) {
      if (!dictIds.add(node.getId())) {
        throw new IllegalStateException("Duplicate dictionary ID found in XML: " + node.getId());
      }
      if (!dictCodes.add(node.getCode())) {
        throw new IllegalStateException(
            "Duplicate dictionary CODE found in XML: " + node.getCode());
      }
      if (!StrUtil.isNotBlank(node.getEnumClass())) {
        throw new IllegalStateException(
            "Dictionary enumClass must not be blank: " + node.getCode());
      }
    }
  }

  @SuppressWarnings("unchecked")
  private List<DictItemSeed> flattenEnumDictItems(DictionaryNode node) throws Exception {
    Class<?> enumClass = Class.forName(node.getEnumClass());
    if (!enumClass.isEnum()) {
      throw new IllegalStateException("enumClass is not an enum: " + node.getEnumClass());
    }
    if (!DictEnumDefinition.class.isAssignableFrom(enumClass)) {
      throw new IllegalStateException(
          "enumClass must implement DictEnumDefinition: " + node.getEnumClass());
    }
    Object[] constants = enumClass.getEnumConstants();
    List<DictItemSeed> result = new ArrayList<>(constants.length);
    Set<String> itemCodes = new HashSet<>();
    for (int i = 0; i < constants.length; i++) {
      Enum<?> constant = (Enum<?>) constants[i];
      DictEnumDefinition definition = (DictEnumDefinition) constant;
      String itemCode = definition.itemCode();
      if (!itemCodes.add(itemCode)) {
        throw new IllegalStateException(
            "Duplicate dictionary item CODE found in enum "
                + node.getEnumClass()
                + ": "
                + itemCode);
      }
      result.add(
          new DictItemSeed(
              node.getCode() + ":" + itemCode,
              null,
              itemCode,
              definition.label(),
              definition.itemValue(),
              i + 1,
              true,
              i == 0,
              blankToNull(definition.tagColor()),
              blankToNull(definition.tagType()),
              null,
              null));
    }
    return result;
  }

  private String blankToNull(String value) {
    return StrUtil.isBlank(value) ? null : value;
  }

  private void validateXml(Resource xmlResource, Resource xsdResource) throws Exception {
    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    try (InputStream xsdInput = xsdResource.getInputStream();
        InputStream xmlInput = xmlResource.getInputStream()) {
      Schema schema = factory.newSchema(new StreamSource(xsdInput));
      Validator validator = schema.newValidator();
      validator.validate(new StreamSource(xmlInput));
    }
  }

  @Getter
  @Setter
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class DictionaryRoot {
    @JacksonXmlProperty(localName = "dictionary")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<DictionaryNode> dictionaries;
  }

  @Getter
  @Setter
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class DictionaryNode {
    private String id;
    private String code;
    private String name;
    private String description;
    private String enumClass;
    private DictValueType valueType;
    private DictStructureType structureType;
    private Boolean enabled;
  }
}
