package com.corwin.framework.xsql.codec;

import jakarta.persistence.AttributeConverter;

/**
 * JPA Convert 编解码器。
 * <p>
 * 复用 JPA {@link jakarta.persistence.AttributeConverter} 完成单列值转换。
 *
 * @author Corwin 2026/4/9
 */
public class XConvertValueCodec implements XValueCodec {

    private final AttributeConverter<Object, Object> converter;

    public XConvertValueCodec(AttributeConverter<Object, Object> converter) {
        this.converter = converter;
    }

    /**
     * Java 值转数据库值。
     */
    @Override
    public Object toDbValue(Object javaValue) {
        if (javaValue == null) {
            return null;
        }
        return converter.convertToDatabaseColumn(javaValue);
    }

    /**
     * 数据库值转 Java 值。
     */
    @Override
    public Object toJavaValue(Object dbValue) {
        if (dbValue == null) {
            return null;
        }
        return converter.convertToEntityAttribute(dbValue);
    }
}

