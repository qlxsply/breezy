package com.corwin.framework.xsql.meta;

import com.corwin.framework.xsql.PhysicalNameResolver;
import com.corwin.framework.xsql.codec.XBasicValueCodec;
import com.corwin.framework.xsql.codec.XConvertValueCodec;
import com.corwin.framework.xsql.codec.XEnumStringCodec;
import com.corwin.framework.xsql.codec.XValueCodec;
import com.corwin.framework.xsql.dialect.XSqlDialect;
import com.corwin.framework.xsql.error.XSqlMetaParseException;
import com.corwin.framework.xsql.error.XSqlUnsupportedEntityException;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SecondaryTables;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * XSql 单表元数据解析器。
 * <p>
 * 设计目标：
 * <ul>
 *     <li>基于 JPA 注解解析主表单列映射。</li>
 *     <li>严格限制支持范围，不支持的结构立即失败。</li>
 *     <li>生成可复用的基础 select/count SQL 与列映射元数据。</li>
 * </ul>
 * 解析失败不应产生半成品结果，必须由调用方保证失败不入缓存。
 *
 * @author Corwin 2026/4/9
 */
public class XEntityMetaParser {

    private final PhysicalNameResolver physicalNameResolver;

    public XEntityMetaParser(PhysicalNameResolver physicalNameResolver) {
        this.physicalNameResolver = physicalNameResolver;
    }

    /**
     * 解析实体元数据。
     *
     * @param entityClass 实体类型
     * @param dialect SQL 方言
     * @return 解析后的实体元数据
     */
    public XEntityMeta parse(Class<?> entityClass, XSqlDialect dialect) {
        ensureEntity(entityClass);
        ensureNoSecondaryTable(entityClass);

        AccessMode accessMode = resolveAccessMode(entityClass);
        LinkedHashMap<String, XColumnMeta> columns = new LinkedHashMap<>();
        parseAttributes(entityClass, accessMode, entityClass, "", Map.of(), columns);
        if (columns.isEmpty()) {
            throw new XSqlMetaParseException("No selectable column found for entity: " + entityClass.getName());
        }

        String logicalTable = resolveLogicalTableName(entityClass);
        String tableName = requireNonBlank(physicalNameResolver.resolveTableName(entityClass, logicalTable),
                "Resolved table name is blank for entity: " + entityClass.getName());
        String tableAlias = "t";

        LinkedHashMap<String, XColumnMeta> aliasColumns = new LinkedHashMap<>();
        StringBuilder select = new StringBuilder("select ");
        int index = 0;
        for (XColumnMeta column : columns.values()) {
            if (!column.selectable()) {
                continue;
            }
            if (index++ > 0) {
                select.append(", ");
            }
            select.append(tableAlias).append('.').append(dialect.quote(column.columnName())).append(" as ")
                    .append(dialect.quote(column.selectAlias()));
            if (aliasColumns.putIfAbsent(column.selectAlias(), column) != null) {
                throw new XSqlMetaParseException(
                        "Duplicate select alias detected: " + column.selectAlias() + " in " + entityClass.getName());
            }
        }
        select.append(" from ").append(dialect.quote(tableName)).append(' ').append(tableAlias);
        String baseSelectSql = select.toString();
        String baseCountSql = "select count(1) from " + dialect.quote(tableName) + " " + tableAlias;

        return new XEntityMeta(entityClass, tableName, tableAlias, baseSelectSql, baseCountSql, columns, columns, columns,
                aliasColumns);
    }

    /**
     * 校验目标类型必须标注 {@link Entity}。
     */
    private void ensureEntity(Class<?> entityClass) {
        if (!entityClass.isAnnotationPresent(Entity.class)) {
            throw new XSqlUnsupportedEntityException("Target class is not @Entity: " + entityClass.getName());
        }
    }

    /**
     * 校验实体未使用 secondary table。
     */
    private void ensureNoSecondaryTable(Class<?> entityClass) {
        if (entityClass.isAnnotationPresent(SecondaryTable.class) || entityClass.isAnnotationPresent(
                SecondaryTables.class)) {
            throw new XSqlUnsupportedEntityException(
                            "Secondary table is not supported in XSql single-table mode: " + entityClass.getName());
        }
    }

    /**
     * 解析访问模式（FIELD / PROPERTY）。
     */
    private AccessMode resolveAccessMode(Class<?> type) {
        Access classAccess = type.getAnnotation(Access.class);
        if (classAccess != null) {
            return classAccess.value() == AccessType.PROPERTY ? AccessMode.PROPERTY : AccessMode.FIELD;
        }

        for (Field field : getAllFields(type)) {
            if (field.isAnnotationPresent(Id.class)) {
                return AccessMode.FIELD;
            }
        }
        for (Method method : type.getMethods()) {
            if (method.isAnnotationPresent(Id.class)) {
                return AccessMode.PROPERTY;
            }
        }
        return AccessMode.FIELD;
    }

    /**
     * 递归解析属性结构并构建列元数据。
     */
    private void parseAttributes(Class<?> type, AccessMode accessMode, Class<?> ownerType, String propertyPrefix,
            Map<String, String> overrideMap, Map<String, XColumnMeta> columns) {
        if (!propertyPrefix.isEmpty() && !type.isAnnotationPresent(Embeddable.class)) {
            throw new XSqlUnsupportedEntityException(
                    "Embedded type must be annotated with @Embeddable: " + type.getName());
        }

        List<MemberMeta> members = readMembers(type, accessMode);
        for (MemberMeta member : members) {
            ensureMemberSupported(member, ownerType, propertyPrefix);
            String propertyPath = propertyPrefix + member.name();
            if (member.hasAnnotation(Embedded.class)) {
                Map<String, String> embeddedOverrideMap = readEmbeddedOverrides(member);
                parseAttributes(member.javaType(), resolveAccessMode(member.javaType()), member.javaType(),
                        propertyPath + ".", embeddedOverrideMap, columns);
                continue;
            }
            XColumnMeta columnMeta = buildColumnMeta(member, ownerType, propertyPath, overrideMap);
            if (columns.putIfAbsent(propertyPath, columnMeta) != null) {
                throw new XSqlMetaParseException("Duplicate property path mapping found: " + propertyPath);
            }
        }
    }

    /**
     * 构建单个属性的列元数据。
     */
    private XColumnMeta buildColumnMeta(MemberMeta member, Class<?> ownerType, String propertyPath,
            Map<String, String> overrideMap) {
        Enumerated enumerated = member.getAnnotation(Enumerated.class);
        Convert convert = member.getAnnotation(Convert.class);

        if (enumerated != null && enumerated.value() != EnumType.STRING) {
            throw new XSqlUnsupportedEntityException(
                    "Only @Enumerated(EnumType.STRING) is supported: " + ownerType.getName() + "#" + propertyPath);
        }
        if (enumerated != null && convert != null && !convert.disableConversion()) {
            throw new XSqlUnsupportedEntityException(
                    "@Enumerated and @Convert cannot be used together: " + ownerType.getName() + "#" + propertyPath);
        }

        XValueCodec codec = resolveCodec(member, enumerated, convert, ownerType, propertyPath);
        String logicalColumn = resolveLogicalColumnName(member, propertyPath, overrideMap);
        String columnName = requireNonBlank(
                physicalNameResolver.resolveColumnName(ownerType, propertyPath, logicalColumn),
                "Resolved column name is blank for property: " + ownerType.getName() + "#" + propertyPath);
        String selectAlias = propertyPath.replace(".", "__");
        return new XColumnMeta(propertyPath, columnName, selectAlias, member.javaType(), codec, true, true, true);
    }

    /**
     * 解析属性编解码策略。
     */
    private XValueCodec resolveCodec(MemberMeta member, Enumerated enumerated, Convert convert, Class<?> ownerType,
            String propertyPath) {
        if (convert != null && !convert.disableConversion()) {
            if (convert.converter() == void.class) {
                throw new XSqlUnsupportedEntityException(
                        "Auto apply @Convert is not supported in XSql, property: " + ownerType.getName() + "#" +
                                propertyPath);
            }
            AttributeConverterHolder holder = createConverter(convert.converter());
            return new XConvertValueCodec(holder.converter());
        }
        if (enumerated != null) {
            if (!member.javaType().isEnum()) {
                throw new XSqlUnsupportedEntityException(
                        "@Enumerated target must be enum type, property: " + ownerType.getName() + "#" + propertyPath);
            }
            @SuppressWarnings("unchecked")
            Class<? extends Enum<?>> enumType = (Class<? extends Enum<?>>) member.javaType();
            return new XEnumStringCodec(enumType);
        }
        return XBasicValueCodec.INSTANCE;
    }

    /**
     * 反射实例化 {@code @Convert} 指定转换器。
     */
    private AttributeConverterHolder createConverter(Class<?> converterType) {
        try {
            Constructor<?> constructor = converterType.getDeclaredConstructor();
            if (!constructor.canAccess(null)) {
                constructor.setAccessible(true);
            }
            Object instance = constructor.newInstance();
            if (!(instance instanceof jakarta.persistence.AttributeConverter<?, ?> attrConverter)) {
                throw new XSqlUnsupportedEntityException(
                        "@Convert converter does not implement AttributeConverter: " + converterType.getName());
            }
            @SuppressWarnings("unchecked")
            jakarta.persistence.AttributeConverter<Object, Object> typed =
                    (jakarta.persistence.AttributeConverter<Object, Object>) attrConverter;
            return new AttributeConverterHolder(typed);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            throw new XSqlUnsupportedEntityException("Instantiate @Convert converter failed: " + converterType.getName());
        }
    }

    /**
     * 解析逻辑表名。
     */
    private String resolveLogicalTableName(Class<?> entityClass) {
        Table table = entityClass.getAnnotation(Table.class);
        if (table != null && !table.name().isBlank()) {
            return table.name().trim();
        }
        Entity entity = entityClass.getAnnotation(Entity.class);
        if (entity != null && !entity.name().isBlank()) {
            return entity.name().trim();
        }
        return null;
    }

    /**
     * 解析显式逻辑列名，优先级：override > @Column。
     * <p>
     * 未显式声明列名时返回 {@code null}，交由 {@link PhysicalNameResolver} 应用运行时命名策略。
     */
    private String resolveLogicalColumnName(MemberMeta member, String propertyPath, Map<String, String> overrideMap) {
        String relativePath = propertyPath;
        int idx = propertyPath.lastIndexOf('.');
        if (idx >= 0 && idx < propertyPath.length() - 1) {
            relativePath = propertyPath.substring(idx + 1);
        }

        String override = overrideMap.get(relativePath);
        if (override != null && !override.isBlank()) {
            return override.trim();
        }
        Column column = member.getAnnotation(Column.class);
        if (column != null && !column.name().isBlank()) {
            return column.name().trim();
        }
        return null;
    }

    /**
     * 读取 embedded 字段上的 {@code @AttributeOverride(s)}。
     */
    private Map<String, String> readEmbeddedOverrides(MemberMeta member) {
        Map<String, String> map = new HashMap<>();
        AttributeOverride single = member.getAnnotation(AttributeOverride.class);
        if (single != null && single.column() != null && !single.column().name().isBlank()) {
            map.put(single.name(), single.column().name().trim());
        }
        AttributeOverrides multi = member.getAnnotation(AttributeOverrides.class);
        if (multi != null) {
            for (AttributeOverride item : multi.value()) {
                if (item.column() != null && !item.column().name().isBlank()) {
                    map.put(item.name(), item.column().name().trim());
                }
            }
        }
        return map;
    }

    /**
     * 校验成员不包含关系映射或集合映射。
     */
    private void ensureMemberSupported(MemberMeta member, Class<?> ownerType, String propertyPrefix) {
        if (member.hasAnnotation(ElementCollection.class) || member.hasAnyAnnotation(OneToOne.class, OneToMany.class,
                ManyToOne.class, ManyToMany.class)) {
            throw new XSqlUnsupportedEntityException(
                    "Relation/collection mapping is not supported: " + ownerType.getName() + "#" + propertyPrefix +
                            member.name());
        }
    }

    /**
     * 按访问模式读取可解析成员。
     */
    private List<MemberMeta> readMembers(Class<?> type, AccessMode accessMode) {
        if (accessMode == AccessMode.FIELD) {
            List<MemberMeta> result = new ArrayList<>();
            Set<String> names = new LinkedHashSet<>();
            for (Field field : getAllFields(type)) {
                if (!names.add(field.getName())) {
                    continue;
                }
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                if (field.isAnnotationPresent(Transient.class)) {
                    continue;
                }
                result.add(new MemberMeta(field.getName(), field.getType(), field, type));
            }
            return result;
        }

        try {
            List<MemberMeta> result = new ArrayList<>();
            for (PropertyDescriptor pd : Introspector.getBeanInfo(type, Object.class).getPropertyDescriptors()) {
                Method read = pd.getReadMethod();
                if (read == null || Modifier.isStatic(read.getModifiers())) {
                    continue;
                }
                if (read.isAnnotationPresent(Transient.class)) {
                    continue;
                }
                result.add(new MemberMeta(pd.getName(), pd.getPropertyType(), read, type));
            }
            return result;
        } catch (IntrospectionException ex) {
            throw new XSqlMetaParseException("Read bean properties failed for: " + type.getName());
        }
    }

    /**
     * 获取类型及其父类的全部字段。
     */
    private List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            Field[] declared = current.getDeclaredFields();
            for (Field field : declared) {
                fields.add(field);
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    /**
     * 校验字符串非空白并返回去空白结果。
     */
    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new XSqlMetaParseException(message);
        }
        return value.trim();
    }

    /**
     * JPA 访问模式。
     */
    private enum AccessMode {
        FIELD,
        PROPERTY
    }

    /**
     * 可解析成员抽象（字段或属性方法）。
     */
    private record MemberMeta(String name, Class<?> javaType, AnnotatedElement element, Class<?> ownerType) {

        /**
         * 判断是否标注指定注解。
         */
        boolean hasAnnotation(Class<? extends Annotation> annotationType) {
            return element.getAnnotation(annotationType) != null;
        }

        /**
         * 判断是否标注任一给定注解。
         */
        @SafeVarargs
        final boolean hasAnyAnnotation(Class<? extends Annotation>... annotationTypes) {
            for (Class<? extends Annotation> annotationType : annotationTypes) {
                if (hasAnnotation(annotationType)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * 获取指定注解实例。
         */
        <A extends Annotation> A getAnnotation(Class<A> annotationType) {
            return element.getAnnotation(annotationType);
        }
    }

    /**
     * Convert 转换器持有对象。
     */
    private record AttributeConverterHolder(jakarta.persistence.AttributeConverter<Object, Object> converter) {
    }
}

