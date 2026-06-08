package com.corwin.framework.xsql.codec;

import com.corwin.framework.xsql.error.XSqlResultMappingException;

/**
 * 枚举字符串编解码器。
 * <p>
 * 对应 {@code @Enumerated(EnumType.STRING)} 语义：
 * 数据库存储枚举名称，读取时按名称反解枚举常量。
 *
 * @author Corwin 2026/4/9
 */
public class XEnumStringCodec implements XValueCodec {

    private final Class<? extends Enum<?>> enumType;

    public XEnumStringCodec(Class<? extends Enum<?>> enumType) {
        this.enumType = enumType;
    }

    /**
     * 枚举编码为名称字符串。
     */
    @Override
    public Object toDbValue(Object javaValue) {
        if (javaValue == null) {
            return null;
        }
        return ((Enum<?>) javaValue).name();
    }

    /**
     * 字符串反解为枚举常量。
     */
    @Override
    public Object toJavaValue(Object dbValue) {
        if (dbValue == null) {
            return null;
        }
        String raw = String.valueOf(dbValue);
        try {
            @SuppressWarnings("unchecked")
            Object value = Enum.valueOf((Class<? extends Enum>) enumType, raw);
            return value;
        } catch (IllegalArgumentException ex) {
            throw new XSqlResultMappingException(
                    "Enum decode failed, type=" + enumType.getName() + ", value=" + raw);
        }
    }
}

