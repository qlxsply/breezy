package com.corwin.framework.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;

/**
 *
 * @author Corwin 2026/1/7
 */
public class InstantMillisDeserializer extends JsonDeserializer<Instant> {

    @Override
    public Instant deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        JsonToken token = jsonParser.currentToken();

        if (token == JsonToken.VALUE_NUMBER_INT) {
            return Instant.ofEpochMilli(jsonParser.getLongValue());
        }

        if (token == JsonToken.VALUE_STRING) {
            String text = jsonParser.getText();
            if (text == null || text.isBlank()) {
                return null;
            }
            try {
                return Instant.ofEpochMilli(Long.parseLong(text.trim()));
            } catch (NumberFormatException e) {
                throw deserializationContext.weirdStringException(text, Instant.class,
                        "Expected epoch milli timestamp");
            }
        }

        if (token == JsonToken.VALUE_NULL) {
            return null;
        }

        return (Instant) deserializationContext.handleUnexpectedToken(Instant.class, jsonParser);
    }

}
