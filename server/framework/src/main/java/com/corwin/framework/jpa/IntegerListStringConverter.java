package com.corwin.framework.jpa;

import com.corwin.framework.constant.TextConstants;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JPA {@link AttributeConverter} that persists {@code List<Integer>} as a comma-separated string.
 * <p>
 * Example: {@code [1,2,3]} is stored as {@code "1,2,3"}.
 */
@Converter
public class IntegerListStringConverter implements AttributeConverter<List<Integer>, String> {

    @Override
    public String convertToDatabaseColumn(List<Integer> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return TextConstants.EMPTY;
        }
        return attribute.stream().map(String::valueOf).collect(Collectors.joining(TextConstants.COMMA));
    }

    @Override
    public List<Integer> convertToEntityAttribute(String dbData) {
        List<Integer> result = new ArrayList<>();

        if (dbData == null || dbData.isBlank()) {
            return result;
        }

        String[] parts = dbData.split(TextConstants.COMMA);
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                try {
                    result.add(Integer.parseInt(trimmed));
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Invalid integer value: " + trimmed, ex);
                }
            }
        }

        return result;
    }
}
