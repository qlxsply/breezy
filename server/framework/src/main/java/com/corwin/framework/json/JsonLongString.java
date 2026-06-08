package com.corwin.framework.json;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 组合注解：将 Long/long 字段序列化为 String，反序列化时将 String 解析为 Long。
 * 主要用于解决前端 JavaScript 处理长整型精度丢失的问题。
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
