package com.corwin.framework.xsql.support;

import com.corwin.framework.xsql.error.XSqlResultMappingException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * XSql Bean 读写工具。
 * <p>
 * 提供结果对象反射访问能力，支持：
 * <ul>
 *     <li>无参构造实例化</li>
 *     <li>setter/getter/field 混合访问</li>
 *     <li>嵌套属性路径自动实例化与写入</li>
 * </ul>
 *
 * @author Corwin 2026/4/9
 */
public final class XSqlBeanAccessor {

    private static final Map<Class<?>, Map<String, Method>> SETTER_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<String, Method>> GETTER_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<String, Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    private XSqlBeanAccessor() {
    }

    /**
     * 通过无参构造创建对象实例。
     */
    public static <T> T newInstance(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            if (!constructor.canAccess(null)) {
                constructor.setAccessible(true);
            }
            return constructor.newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
                    throw new XSqlResultMappingException("Result type must have no-arg constructor: " + type.getName());
        }
    }

    /**
     * 写入属性路径值，支持自动创建中间嵌套对象。
     */
    public static void setPropertyPath(Object target, String propertyPath, Object rawValue) {
        if (target == null) {
            throw new XSqlResultMappingException("Target object is null");
        }
        if (propertyPath == null || propertyPath.isBlank()) {
            throw new XSqlResultMappingException("Property path is blank");
        }
        String[] parts = propertyPath.split("\\.");
        Object current = target;
        for (int i = 0; i < parts.length - 1; i++) {
            String segment = parts[i];
            Object nested = readProperty(current, segment);
            if (nested == null) {
                Class<?> nestedType = resolvePropertyType(current.getClass(), segment);
                nested = newInstance(nestedType);
                writeProperty(current, segment, nested);
            }
            current = nested;
        }
        String last = parts[parts.length - 1];
        Class<?> targetType = resolvePropertyType(current.getClass(), last);
        Object converted = XSqlTypeConverter.convert(rawValue, targetType);
        writeProperty(current, last, converted);
    }

    /**
     * 读取单级属性值（getter 优先，字段兜底）。
     */
    private static Object readProperty(Object target, String property) {
        Method getter = resolveGetter(target.getClass(), property);
        if (getter != null) {
            try {
                return getter.invoke(target);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new XSqlResultMappingException("Invoke getter failed: " + target.getClass().getName() + "." + property);
            }
        }
        Field field = resolveField(target.getClass(), property);
        if (field != null) {
            try {
                return field.get(target);
            } catch (IllegalAccessException ex) {
                throw new XSqlResultMappingException("Read field failed: " + target.getClass().getName() + "." + property);
            }
        }
        throw new XSqlResultMappingException("Property not found: " + target.getClass().getName() + "." + property);
    }

    /**
     * 写入单级属性值（setter 优先，字段兜底）。
     */
    private static void writeProperty(Object target, String property, Object value) {
        Method setter = resolveSetter(target.getClass(), property);
        if (setter != null) {
            try {
                setter.invoke(target, value);
                return;
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new XSqlResultMappingException("Invoke setter failed: " + target.getClass().getName() + "." + property);
            }
        }
        Field field = resolveField(target.getClass(), property);
        if (field != null) {
            try {
                field.set(target, value);
                return;
            } catch (IllegalAccessException ex) {
                throw new XSqlResultMappingException("Write field failed: " + target.getClass().getName() + "." + property);
            }
        }
        throw new XSqlResultMappingException("Writable property not found: " + target.getClass().getName() + "." + property);
    }

    /**
     * 解析属性类型（setter -> 字段 -> getter）。
     */
    private static Class<?> resolvePropertyType(Class<?> type, String property) {
        Method setter = resolveSetter(type, property);
        if (setter != null && setter.getParameterCount() == 1) {
            return setter.getParameterTypes()[0];
        }
        Field field = resolveField(type, property);
        if (field != null) {
            return field.getType();
        }
        Method getter = resolveGetter(type, property);
        if (getter != null) {
            return getter.getReturnType();
        }
        throw new XSqlResultMappingException("Property type not found: " + type.getName() + "." + property);
    }

    /**
     * 解析 setter 方法缓存。
     */
    private static Method resolveSetter(Class<?> type, String property) {
        Map<String, Method> setterMap = SETTER_CACHE.computeIfAbsent(type, XSqlBeanAccessor::scanSetters);
        return setterMap.get(property);
    }

    /**
     * 解析 getter 方法缓存。
     */
    private static Method resolveGetter(Class<?> type, String property) {
        Map<String, Method> getterMap = GETTER_CACHE.computeIfAbsent(type, XSqlBeanAccessor::scanGetters);
        return getterMap.get(property);
    }

    /**
     * 解析字段缓存。
     */
    private static Field resolveField(Class<?> type, String property) {
        Map<String, Field> fieldMap = FIELD_CACHE.computeIfAbsent(type, XSqlBeanAccessor::scanFields);
        return fieldMap.get(property);
    }

    /**
     * 扫描可写 setter。
     */
    private static Map<String, Method> scanSetters(Class<?> type) {
        Map<String, Method> map = new ConcurrentHashMap<>();
        for (Method method : type.getMethods()) {
            if (!method.getName().startsWith("set") || method.getParameterCount() != 1 || Modifier.isStatic(
                    method.getModifiers())) {
                continue;
            }
            String name = decapitalize(method.getName().substring(3));
            map.putIfAbsent(name, method);
        }
        return map;
    }

    /**
     * 扫描可读 getter。
     */
    private static Map<String, Method> scanGetters(Class<?> type) {
        Map<String, Method> map = new ConcurrentHashMap<>();
        for (Method method : type.getMethods()) {
            if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 0) {
                continue;
            }
            String name = method.getName();
            if ("getClass".equals(name)) {
                continue;
            }
            String property = null;
            if (name.startsWith("get") && name.length() > 3) {
                property = decapitalize(name.substring(3));
            } else if (name.startsWith("is") && name.length() > 2) {
                property = decapitalize(name.substring(2));
            }
            if (property != null) {
                map.putIfAbsent(property, method);
            }
        }
        return map;
    }

    /**
     * 扫描全部实例字段（含父类）。
     */
    private static Map<String, Field> scanFields(Class<?> type) {
        Map<String, Field> map = new ConcurrentHashMap<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                map.putIfAbsent(field.getName(), field);
            }
            current = current.getSuperclass();
        }
        return map;
    }

    /**
     * JavaBean 首字母小写化。
     */
    private static String decapitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.length() > 1 && Character.isUpperCase(value.charAt(1)) && Character.isUpperCase(value.charAt(0))) {
            return value;
        }
        char[] chars = value.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }
}
