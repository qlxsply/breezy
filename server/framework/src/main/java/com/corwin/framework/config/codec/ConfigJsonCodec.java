package com.corwin.framework.config.codec;

import com.corwin.framework.config.error.ConfigLoadException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * @author Corwin 2026/7/30
 */
public final class ConfigJsonCodec {

  private static final ObjectMapper MAPPER = createMapper();

  public static String serialize(Object value) {
    try {
      return MAPPER.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      throw new ConfigLoadException("Failed to serialize config value", ex);
    }
  }

  public static <T> T deserialize(String content, Class<T> valueClass) {
    if (content == null || content.isBlank()) {
      throw new ConfigLoadException("Config content must not be blank");
    }
    try {
      return MAPPER.readValue(content, valueClass);
    } catch (JsonProcessingException ex) {
      throw new ConfigLoadException(
          "Failed to deserialize config value as " + valueClass.getName(), ex);
    }
  }

  public static JsonNode readTree(String content) {
    if (content == null || content.isBlank()) {
      throw new ConfigLoadException("Config content must not be blank");
    }
    try {
      return MAPPER.readTree(content);
    } catch (JsonProcessingException ex) {
      throw new ConfigLoadException("Failed to parse config JSON", ex);
    }
  }

  public static <T> T treeToValue(JsonNode value, Class<T> valueClass) {
    if (value == null || value.isNull()) {
      throw new ConfigLoadException("Config value must not be null");
    }
    try {
      return MAPPER.treeToValue(value, valueClass);
    } catch (JsonProcessingException ex) {
      throw new ConfigLoadException(
          "Failed to deserialize config value as " + valueClass.getName(), ex);
    }
  }

  public static JsonNode valueToTree(Object value) {
    if (value == null) {
      throw new ConfigLoadException("Config value must not be null");
    }
    return MAPPER.valueToTree(value);
  }

  public static ObjectMapper mapper() {
    return MAPPER.copy();
  }

  private static ObjectMapper createMapper() {
    return new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  private ConfigJsonCodec() {}
}
