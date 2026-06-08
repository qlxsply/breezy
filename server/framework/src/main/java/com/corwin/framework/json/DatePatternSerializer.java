package com.corwin.framework.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 *
 * @author Corwin 2026/1/7
 */
public class DatePatternSerializer extends JsonSerializer<Date> {

    private final String pattern;

    DatePatternSerializer(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public void serialize(Date date, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException {
        if (date == null) {
            jsonGenerator.writeNull();
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        sdf.setTimeZone(TimeZone.getDefault());
        jsonGenerator.writeString(sdf.format(date));
    }

}
