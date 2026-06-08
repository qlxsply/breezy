package com.corwin.framework.xsql.support;

import com.corwin.framework.xsql.error.XSqlResultMappingException;

import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.util.Locale;

/**
 * XSql 基础类型转换工具。
 * <p>
 * 用于结果映射阶段把 JDBC 返回值转换为目标属性类型。
 * 支持枚举、数字、布尔、Instant 等常见转换。
 *
 * @author Corwin 2026/4/9
 */
public final class XSqlTypeConverter {

    private XSqlTypeConverter() {
    }

    /**
     * 转换值到目标类型。
     */
    public static Object convert(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        if (targetType == null || targetType == Object.class) {
            return value;
        }
        Class<?> boxedTarget = box(targetType);
        if (boxedTarget.isInstance(value)) {
            return value;
        }

        if (boxedTarget.isEnum()) {
            String raw = String.valueOf(value);
            @SuppressWarnings("unchecked") Class<? extends Enum> enumType = (Class<? extends Enum>) boxedTarget;
            try {
                return Enum.valueOf(enumType, raw);
            } catch (IllegalArgumentException ex) {
                throw new XSqlResultMappingException(
                        "Enum convert failed, type=" + boxedTarget.getName() + ", value=" + raw);
            }
        }

        if (boxedTarget == String.class) {
            return String.valueOf(value);
        }
        if (boxedTarget == Instant.class) {
            return toInstant(value);
        }
        if (Number.class.isAssignableFrom(boxedTarget)) {
            return toNumber(value, boxedTarget);
        }
        if (boxedTarget == Boolean.class) {
            return toBoolean(value);
        }
        return value;
    }

    /**
     * 数字类型转换。
     */
    private static Object toNumber(Object value, Class<?> numberType) {
        BigDecimal decimal;
        if (value instanceof Number number) {
            decimal = new BigDecimal(number.toString());
        } else {
            decimal = new BigDecimal(String.valueOf(value));
        }
        if (numberType == Integer.class) {
            return decimal.intValue();
        }
        if (numberType == Long.class) {
            return decimal.longValue();
        }
        if (numberType == Short.class) {
            return decimal.shortValue();
        }
        if (numberType == Byte.class) {
            return decimal.byteValue();
        }
        if (numberType == Double.class) {
            return decimal.doubleValue();
        }
        if (numberType == Float.class) {
            return decimal.floatValue();
        }
        if (numberType == BigDecimal.class) {
            return decimal;
        }
        return value;
    }

    /**
     * 布尔值转换。
     */
    private static Object toBoolean(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String raw = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        return "1".equals(raw) || "true".equals(raw) || "y".equals(raw) || "yes".equals(raw);
    }

    /**
     * Instant 类型转换。
     */
    private static Object toInstant(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant();
        }
        if (value instanceof Time sqlTime) {
            return sqlTime.toLocalTime().atDate(LocalDate.ofEpochDay(0)).atZone(ZoneId.systemDefault()).toInstant();
        }
        if (value instanceof java.util.Date date) {
            return date.toInstant();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        }
        if (value instanceof LocalDate localDate) {
            return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        if (value instanceof ZonedDateTime zonedDateTime) {
            return zonedDateTime.toInstant();
        }
        if (value instanceof Number number) {
            return Instant.ofEpochMilli(number.longValue());
        }

        String raw = String.valueOf(value).trim();

        try {
            return Instant.ofEpochMilli(Long.parseLong(raw));
        } catch (NumberFormatException ignore) {
            // ignore
        }

        try {
            return Instant.parse(raw);
        } catch (DateTimeException ignore) {
            // ignore
        }

        try {
            return OffsetDateTime.parse(raw).toInstant();
        } catch (DateTimeException ignore) {
            // ignore
        }

        try {
            return LocalDateTime.parse(raw.replace(' ', 'T')).atZone(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeException ex) {
            throw new XSqlResultMappingException(
                    "Instant convert failed, value=" + raw + ", type=" + value.getClass().getName());
        }
    }

    /**
     * 基础类型装箱。
     */
    private static Class<?> box(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return type;
    }
}

