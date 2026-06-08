package com.corwin.framework.xsql.codec;

/**
 * XSql 基础编解码器。
 * <p>
 * 用于无需转换的字段，输入输出保持原值。
 *
 * @author Corwin 2026/4/9
 */
public class XBasicValueCodec implements XValueCodec {

    public static final XBasicValueCodec INSTANCE = new XBasicValueCodec();

    private XBasicValueCodec() {
    }

    /**
     * 直接返回原值。
     */
    @Override
    public Object toDbValue(Object javaValue) {
        return javaValue;
    }

    /**
     * 直接返回原值。
     */
    @Override
    public Object toJavaValue(Object dbValue) {
        return dbValue;
    }
}

