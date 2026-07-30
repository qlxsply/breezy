package com.corwin.framework.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Jackson deserializer for {@link java.util.Date} using a configurable date-time pattern.
 *
 * @author Corwin 2026/1/7
 */
public class DatePatternDeserializer extends JsonDeserializer<Date> {

    private final String pattern;

    DatePatternDeserializer(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public Date deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        String text = jsonParser.getValueAsString();
        if (text == null || text.isBlank()) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        sdf.setTimeZone(TimeZone.getDefault());
        try {
            return sdf.parse(text.trim());
        } catch (ParseException e) {
            throw deserializationContext.weirdStringException(text, Date.class,
                    "Invalid date format, expected: " + pattern);
        }
    }

}
