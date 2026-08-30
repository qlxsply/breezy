package com.corwin.framework.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.time.Instant;

/**
 * Jackson serializer for {@link java.time.Instant} that outputs epoch-millisecond values as
 * strings.
 *
 * @author Corwin 2026/1/7
 */
public class InstantMillisSerializer extends JsonSerializer<Instant> {

  @Override
  public void serialize(
      Instant instant, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
      throws IOException {
    if (instant == null) {
      jsonGenerator.writeNull();
      return;
    }
    jsonGenerator.writeString(String.valueOf(instant.toEpochMilli()));
  }
}
