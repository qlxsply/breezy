package com.corwin.framework.xsql.codec;

/**
 * XSql 值编解码器。
 * <p>
 * 用于在“Java 属性值”与“数据库单列值”之间进行双向转换。
 * 典型场景包括：
 * <ul>
 *     <li>基础类型直通（不转换）</li>
 *     <li>枚举字符串映射（{@code EnumType.STRING}）</li>
 *     <li>JPA {@code @Convert} 自定义转换</li>
 * </ul>
 * 该接口仅处理“单值映射”，不承担集合语义解析。
 *
 * @author Corwin 2026/4/9
 */
public interface XValueCodec {

    /**
     * 将 Java 值转换为可用于 SQL 参数绑定的数据库值。
     *
     * @param javaValue Java 侧属性值
     * @return 数据库侧值
     */
    Object toDbValue(Object javaValue);

    /**
     * 将数据库值转换为 Java 侧属性值。
     *
     * @param dbValue 数据库侧值
     * @return Java 侧值
     */
    Object toJavaValue(Object dbValue);
}

