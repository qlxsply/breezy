package com.corwin.framework.util;

/**
 *
 * @author wl0180 2026/6/15
 */
public class Defaults {

    private Defaults() {
    }

    public static <T> T or(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    public static int or(Integer value, int defaultValue) {
        return value != null ? value : defaultValue;
    }

    public static long or(Long value, long defaultValue) {
        return value != null ? value : defaultValue;
    }

    public static boolean or(Boolean value, boolean defaultValue) {
        return value != null ? value : defaultValue;
    }

    public static double or(Double value, double defaultValue) {
        return value != null ? value : defaultValue;
    }

    public static float or(Float value, float defaultValue) {
        return value != null ? value : defaultValue;
    }

    public static short or(Short value, short defaultValue) {
        return value != null ? value : defaultValue;
    }

    public static byte or(Byte value, byte defaultValue) {
        return value != null ? value : defaultValue;
    }

    public static char or(Character value, char defaultValue) {
        return value != null ? value : defaultValue;
    }

    public static String or(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }

}
