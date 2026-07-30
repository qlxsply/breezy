package com.corwin.framework.json;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Composite annotation that serializes {@code Long} / {@code long} as String and deserializes String back to Long.
 * <p>
 * This addresses the precision loss issue that occurs when JavaScript consumes large
 * numeric values that exceed Number.MAX_SAFE_INTEGER.
 *
 * @author Corwin 2026/2/10
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@JacksonAnnotationsInside
@JsonSerialize(using = LongAsStringSerializer.class)
@JsonDeserialize(using = StringAsLongDeserializer.class)
public @interface JsonLongString {
}
