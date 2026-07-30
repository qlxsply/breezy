package com.corwin.framework.util;

/**
 * Null-safe fallback utilities for retrieving a value or a default.
 * <p>
 * Provides overloaded {@link #or(Object, Object)} methods for common types,
 * allowing concise inline default-value expressions without manual null checks.
 *
 * @author wl0180 2026/6/15
 */
public class Defaults {

    private Defaults() {
    }

    /**
     * Returns {@code value} if non-null, otherwise {@code defaultValue}.
     *
     * @param <T>          the value type
     * @param value        the value to evaluate, may be null
     * @param defaultValue the fallback value
     * @return value or defaultValue
     */
    public static <T> T or(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Null-safe unboxing fallback for {@link Integer}.
     *
     * @param value        the value to evaluate, may be null
     * @param defaultValue the fallback primitive
     * @return the unboxed value or defaultValue
     */
    public static int or(Integer value, int defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Null-safe unboxing fallback for {@link Long}.
     *
     * @param value        the value to evaluate, may be null
     * @param defaultValue the fallback primitive
     * @return the unboxed value or defaultValue
     */
    public static long or(Long value, long defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Null-safe unboxing fallback for {@link Boolean}.
     *
     * @param value        the value to evaluate, may be null
     * @param defaultValue the fallback primitive
     * @return the unboxed value or defaultValue
     */
    public static boolean or(Boolean value, boolean defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Null-safe unboxing fallback for {@link Double}.
     *
     * @param value        the value to evaluate, may be null
     * @param defaultValue the fallback primitive
     * @return the unboxed value or defaultValue
     */
    public static double or(Double value, double defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Null-safe unboxing fallback for {@link Float}.
     *
     * @param value        the value to evaluate, may be null
     * @param defaultValue the fallback primitive
     * @return the unboxed value or defaultValue
     */
    public static float or(Float value, float defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Null-safe unboxing fallback for {@link Short}.
     *
     * @param value        the value to evaluate, may be null
     * @param defaultValue the fallback primitive
     * @return the unboxed value or defaultValue
     */
    public static short or(Short value, short defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Null-safe unboxing fallback for {@link Byte}.
     *
     * @param value        the value to evaluate, may be null
     * @param defaultValue the fallback primitive
     * @return the unboxed value or defaultValue
     */
    public static byte or(Byte value, byte defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Null-safe unboxing fallback for {@link Character}.
     *
     * @param value        the value to evaluate, may be null
     * @param defaultValue the fallback primitive
     * @return the unboxed value or defaultValue
     */
    public static char or(Character value, char defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Null-safe fallback for {@link String}.
     *
     * @param value        the value to evaluate, may be null
     * @param defaultValue the fallback value
     * @return value or defaultValue
     */
    public static String or(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }

}
