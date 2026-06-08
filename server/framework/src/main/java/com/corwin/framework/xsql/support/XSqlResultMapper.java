package com.corwin.framework.xsql.support;

import com.corwin.framework.xsql.XColumn;
import com.corwin.framework.xsql.codec.XValueCodec;
import com.corwin.framework.xsql.error.XSqlResultMappingException;
import com.corwin.framework.xsql.meta.XColumnMeta;
import com.corwin.framework.xsql.meta.XEntityMeta;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * XSql 结果映射器。
 * <p>
 * 提供两种映射模式：
 * <ul>
 *     <li>单表模式：基于元数据 aliasColumns 映射。</li>
 *     <li>原生模式：基于 {@code @XColumn} / 字段名映射。</li>
 * </ul>
 *
 * @author Corwin 2026/4/9
 */
public final class XSqlResultMapper {

    private XSqlResultMapper() {
    }

    /**
     * 映射单表查询结果行。
     */
    public static <R> R mapTableRow(ResultSet rs, XEntityMeta meta, Class<R> resultClass) throws SQLException {
        R target = XSqlBeanAccessor.newInstance(resultClass);
        for (XColumnMeta columnMeta : meta.aliasColumns().values()) {
            Object raw = rs.getObject(columnMeta.selectAlias());
            XValueCodec codec = columnMeta.valueCodec();
            Object decoded = codec == null ? raw : codec.toJavaValue(raw);
            XSqlBeanAccessor.setPropertyPath(target, columnMeta.propertyPath(), decoded);
        }
        return target;
    }

    /**
     * 映射原生查询结果行。
     */
    public static <R> R mapNativeRow(ResultSet rs, Class<R> resultClass, Map<String, String> aliasToProperty)
            throws SQLException {
        R target = XSqlBeanAccessor.newInstance(resultClass);
        ResultSetMetaData metaData = rs.getMetaData();
        int count = metaData.getColumnCount();
        for (int i = 1; i <= count; i++) {
            String label = metaData.getColumnLabel(i);
            String property = aliasToProperty.get(normalize(label));
            if (property == null) {
                throw new XSqlResultMappingException(
                        "Alias not found in result class mapping: " + label + ", resultType=" + resultClass.getName());
            }
            Object value = rs.getObject(i);
            XSqlBeanAccessor.setPropertyPath(target, property, value);
        }
        return target;
    }

    /**
     * 构建原生查询 alias 映射表。
     */
    public static Map<String, String> buildNativeAliasMap(Class<?> resultClass) {
        Map<String, String> aliasMap = new HashMap<>();
        Class<?> current = resultClass;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                XColumn annotation = field.getAnnotation(XColumn.class);
                String alias = annotation == null ? field.getName() : annotation.value();
                if (alias == null || alias.isBlank()) {
                    throw new XSqlResultMappingException(
                            "@XColumn alias must not be blank, field=" + field.getName() + ", type=" +
                                    resultClass.getName());
                }
                aliasMap.putIfAbsent(normalize(alias), field.getName());
            }
            current = current.getSuperclass();
        }
        if (aliasMap.isEmpty()) {
            throw new XSqlResultMappingException("No mappable field found in native result type: " + resultClass.getName());
        }
        return aliasMap;
    }

    /**
     * alias 归一化（trim + 小写）。
     */
    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

