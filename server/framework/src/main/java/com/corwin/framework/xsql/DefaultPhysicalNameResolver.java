package com.corwin.framework.xsql;

/**
 * 默认物理命名解析器。
 * <p>
 * 规则：
 * <ul>
 *     <li>显式命名与默认命名统一按简单驼峰转下划线推导。</li>
 *     <li>未显式表名时，以实体简单类名作为默认逻辑表名。</li>
 *     <li>未显式列名时，以属性路径最后一段作为默认逻辑列名。</li>
 * </ul>
 * 该实现仅作为非 Hibernate 场景下的兜底命名器，生产环境建议使用与 JPA Provider 对齐的实现。
 *
 * @author Corwin 2026/4/9
 */
public class DefaultPhysicalNameResolver implements PhysicalNameResolver {

    /**
     * 解析表名。
     */
    @Override
    public String resolveTableName(Class<?> entityClass, String logicalTableName) {
        String logicalName = logicalTableName;
        if (logicalName == null || logicalName.isBlank()) {
            logicalName = entityClass == null ? null : entityClass.getSimpleName();
        }
        return toSnakeCase(logicalName == null ? null : logicalName.trim());
    }

    /**
     * 解析列名。
     */
    @Override
    public String resolveColumnName(Class<?> ownerType, String propertyPath, String logicalColumnName) {
        String logicalName = logicalColumnName;
        if (logicalName == null || logicalName.isBlank()) {
            logicalName = propertyPath;
            int idx = propertyPath == null ? -1 : propertyPath.lastIndexOf('.');
            if (idx >= 0 && idx < propertyPath.length() - 1) {
                logicalName = propertyPath.substring(idx + 1);
            }
        }
        return toSnakeCase(logicalName == null ? null : logicalName.trim());
    }

    /**
     * 驼峰转下划线。
     */
    private String toSnakeCase(String source) {
        if (source == null || source.isBlank()) {
            return source;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(ch));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}

