package com.corwin.framework.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import org.springframework.util.StringUtils;

/**
 * Jackson deserializer that parses string-wrapped or raw numeric JSON values into {@link Long}.
 *
 * @author Corwin 2026/2/10
 */
public class StringAsLongDeserializer extends JsonDeserializer<Long> {

  @Override
  public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    String value = p.getText();
    if (!StringUtils.hasText(value)) {
      return null;
    }
    try {
      return Long.valueOf(value.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
