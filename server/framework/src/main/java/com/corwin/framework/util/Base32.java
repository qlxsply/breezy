package com.corwin.framework.util;

/**
 * Utility for custom base‑32 encoding/decoding.
 * <p>
 * Uses a 32‑character alphabet that omits visually ambiguous letters
 * (e.g. g, l, o, q in lowercase; I, O, U, V in uppercase).
 * Supports conversion between integral values and fixed‑width strings
 * with optional zero padding.
 * </p>
 *
 * <h3>Potential issues/notices:</h3>
 * <ul>
 *   <li><strong>Character set choice:</strong> This is not RFC4648 base32—
 * branded applications may be confused if they expect canonical base32.</li>
 *   <li><strong>Fixed buffer size:</strong> toStr(long,…) computes length by
 * bit‑width; for very small values plus large minScale you may get leading zeros.</li>
 *   <li><strong>Index arrays size:</strong> uses 128‑length int[]; non‑ASCII input will
 * result in {@code ArrayIndexOutOfBoundsException}—consider validating/limiting to [0..127].</li>
 *   <li><strong>Negative numbers:</strong> toStr assumes unsigned behavior (logical shift);
 * longFromStr treats input as unsigned too. If negative inputs occur, results are undefined.</li>
 * </ul>
 *
 * @author Corwin 2025/5/12
 */
public class Base32 {

    /**
     * Convert a long to lowercase base‑32 string, no padding.
     */
    public static String toLowStr(long val) {
        return toStr(val, LOWERCASE_CODE, 0);
    }

    /**
     * Convert a long to lowercase base‑32 string, padded to at least minScale chars.
     */
    public static String toLowStr(long val, int minScale) {
        return toStr(val, LOWERCASE_CODE, minScale);
    }

    /**
     * Parse a lowercase base‑32 string back to a long.
     */
    public static long longFromLowStr(String str) {
        return longFromStr(str, LOWERCASE_INDEX);
    }

    /**
     * Convert a long to uppercase base‑32 string, no padding.
     */
    public static String toUpStr(long val) {
        return toStr(val, UPPERCASE_CODE, 0);
    }

    /**
     * Convert a long to uppercase base‑32 string, padded to at least minScale chars.
     */
    public static String toUpStr(long val, int minScale) {
        return toStr(val, UPPERCASE_CODE, minScale);
    }

    /**
     * Parse an uppercase base‑32 string back to a long.
     */
    public static long longFromUpStr(String str) {
        return longFromStr(str, UPPERCASE_INDEX);
    }

    /**
     * Convert an int to lowercase base‑32 string, no padding.
     */
    public static String toLowStr(int val) {
        return toStr(val, LOWERCASE_CODE, 0);
    }

    /**
     * Convert an int to lowercase base‑32 string, padded to at least minScale chars.
     */
    public static String toLowStr(int val, int minScale) {
        return toStr(val, LOWERCASE_CODE, minScale);
    }

    /**
     * Parse a lowercase base‑32 string back to an int.
     */
    public static int intFromLowStr(String str) {
        return intFromStr(str, LOWERCASE_INDEX);
    }

    /**
     * Convert an int to uppercase base‑32 string, no padding.
     */
    public static String toUpStr(int val) {
        return toStr(val, UPPERCASE_CODE, 0);
    }

    /**
     * Convert an int to uppercase base‑32 string, padded to at least minScale chars.
     */
    public static String toUpStr(int val, int minScale) {
        return toStr(val, UPPERCASE_CODE, minScale);
    }

    /**
     * Parse an uppercase base‑32 string back to an int.
     */
    public static int intFromUpStr(String str) {
        return intFromStr(str, UPPERCASE_INDEX);
    }

    /**
     * Lowercase alphabet (32 chars) omitting g, l, o, q to avoid visual ambiguity.
     */
    private final static char[] LOWERCASE_CODE = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
            'e', 'f', 'h', 'i', 'j', 'k', 'm', 'n', 'p', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};

    /**
     * Uppercase alphabet (32 chars) omitting I, O, U, V to avoid visual ambiguity.
     */
    private final static char[] UPPERCASE_CODE = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
            'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'W', 'X', 'Y', 'Z'};

    /**
     * Lookup table for lowercase chars → index.
     */
    private final static int[] LOWERCASE_INDEX = new int[128];

    /**
     * Lookup table for uppercase chars → index.
     */
    private final static int[] UPPERCASE_INDEX = new int[128];

    // Initialize index tables
    static {
        for (int i = 0; i < LOWERCASE_CODE.length; i++) {
            LOWERCASE_INDEX[LOWERCASE_CODE[i]] = i;
        }
        for (int i = 0; i < UPPERCASE_CODE.length; i++) {
            UPPERCASE_INDEX[UPPERCASE_CODE[i]] = i;
        }
    }

    /**
     * Convert an unsigned long to a custom base‑32 string.
     *
     * @param val      the input value (treated as unsigned)
     * @param code     the 32‑char alphabet
     * @param minScale minimum output length (left‑pad with code[0] if shorter)
     * @return encoded string
     */
    private static String toStr(long val, char[] code, int minScale) {
        int mag = Long.SIZE - Long.numberOfLeadingZeros(val);
        int cl = Math.max(((mag + 4) / 5), minScale);
        char[] buf = new char[cl];
        int mask = (1 << 5) - 1;
        do {
            buf[--cl] = code[((int) val) & mask];
            val >>>= 5;
        } while (val != 0 && cl > 0);
        while (cl > 0) {
            buf[--cl] = code[0];
        }
        return new String(buf);
    }

    /**
     * Convert an unsigned int to a custom base‑32 string.
     *
     * @param val      the input value
     * @param code     the 32‑char alphabet
     * @param minScale minimum output length
     * @return encoded string
     */
    private static String toStr(int val, char[] code, int minScale) {
        int mag = Integer.SIZE - Integer.numberOfLeadingZeros(val);
        int cl = Math.max(((mag + 4) / 5), minScale);
        char[] buf = new char[cl];
        int mask = (1 << 5) - 1;
        do {
            buf[--cl] = code[val & mask];
            val >>>= 5;
        } while (val != 0 && cl > 0);
        while (cl > 0) {
            buf[--cl] = code[0];
        }
        return new String(buf);
    }

    /**
     * Decode a custom base‑32 string to a long.
     *
     * @param str   the encoded string
     * @param index lookup table mapping chars to values
     * @return decoded long
     */
    private static long longFromStr(String str, int[] index) {
        long result = 0;
        for (char c : str.toCharArray()) {
            result = (result << 5) | index[c];
        }
        return result;
    }

    /**
     * Decode a custom base‑32 string to an int.
     *
     * @param str   the encoded string
     * @param index lookup table mapping chars to values
     * @return decoded int
     */
    private static int intFromStr(String str, int[] index) {
        int result = 0;
        for (char c : str.toCharArray()) {
            result = (result << 5) | index[c];
        }
        return result;
    }

}
