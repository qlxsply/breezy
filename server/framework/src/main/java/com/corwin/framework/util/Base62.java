package com.corwin.framework.util;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Utility for custom base‑62 encoding/decoding.
 *
 * <p>Uses the 62-character alphabet [0-9][A-Z][a-z]. Supports conversion between integral values
 * and fixed‑width strings with optional zero padding. Rejects negative inputs and invalid chars.
 *
 * <h3>Potential issues/notices:</h3>
 *
 * <ul>
 *   <li><strong>Unsupported negative values:</strong> {@link #toBase62} throws
 *       IllegalArgumentException for negatives.
 *   <li><strong>Invalid chars:</strong> {@link #parseBase62} throws when encountering non‑alphabet
 *       characters or overflow.
 *   <li><strong>Padding:</strong> minScale pads with ‘0’, which may be ambiguous if leading zeros
 *       are significant.
 * </ul>
 *
 * @author Corwin 2025/5/12
 */
public class Base62 {

  /** Convert a long to Base62 string with no padding. */
  public static String toStr(long val) {
    return toStr(val, 0);
  }

  /** Convert a long to Base62 string, padded to at least minScale chars. */
  public static String toStr(long val, int minScale) {
    return toBase62(val, minScale);
  }

  /** Parse a Base62 string back to a long. */
  public static long longFromStr(String str) {
    return parseBase62(str);
  }

  /** Convert an int to Base62 string with no padding. */
  public static String toStr(int val) {
    return toStr((long) val, 0);
  }

  /** Convert an int to Base62 string, padded to at least minScale chars. */
  public static String toStr(int val, int minScale) {
    return toStr((long) val, minScale);
  }

  /** Parse a Base62 string back to an int (throws if overflow). */
  public static int intFromStr(String str) {
    long result = parseBase62(str);
    if (result > Integer.MAX_VALUE) {
      throw new NumberFormatException("Value exceeds Integer.MAX_VALUE");
    }
    return (int) result;
  }

  /** Converts a non-negative BigInteger to Base62 using our custom alphabet */
  public static String toStr(BigInteger number) {
    StringBuilder sb = new StringBuilder();
    while (number.compareTo(BigInteger.ZERO) > 0) {
      BigInteger[] divRem = number.divideAndRemainder(BASE);
      number = divRem[0];
      int digit = divRem[1].intValue();
      sb.append(BASE62_CODE[digit]);
    }
    return sb.reverse().toString();
  }

  /** Alphabet for Base62: digits, uppercase, then lowercase letters. */
  private static final char[] BASE62_CODE = {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I',
    'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b',
    'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u',
    'v', 'w', 'x', 'y', 'z'
  };

  /** Base value for encoding */
  private static final BigInteger BASE = BigInteger.valueOf(BASE62_CODE.length);

  /** Reverse lookup table: ASCII → index in BASE62_CODE, -1 if invalid. */
  private static final int[] BASE62_INDEX = new int[128];

  static {
    Arrays.fill(BASE62_INDEX, -1);
    for (int i = 0; i < BASE62_CODE.length; i++) {
      BASE62_INDEX[BASE62_CODE[i]] = i;
    }
  }

  /**
   * Core encoding: convert a non‑negative long to Base62.
   *
   * @param val input value (must be ≥ 0)
   * @param minScale minimum length (pad with '0' on left)
   * @return encoded string
   */
  private static String toBase62(long val, int minScale) {
    if (val < 0) {
      throw new IllegalArgumentException("Negative values not supported");
    }
    if (val == 0) {
      return minScale > 0 ? repeat(minScale) : "0";
    }
    int len = 0;
    long tmp = val;
    while (tmp != 0) {
      tmp /= 62;
      len++;
    }
    int finalLen = Math.max(len, minScale);
    char[] buf = new char[finalLen];
    int idx = finalLen - 1;
    while (val != 0) {
      buf[idx--] = BASE62_CODE[(int) (val % 62)];
      val /= 62;
    }
    while (idx >= 0) {
      buf[idx--] = '0';
    }
    return new String(buf);
  }

  /**
   * Core decoding: parse Base62 string to a long.
   *
   * @param str the encoded string
   * @return decoded value
   * @throws IllegalArgumentException if invalid character found
   * @throws ArithmeticException if result overflows long
   */
  private static long parseBase62(String str) {
    long result = 0;
    for (char c : str.toCharArray()) {
      if (c >= BASE62_INDEX.length || BASE62_INDEX[c] < 0) {
        throw new IllegalArgumentException("Invalid Base62 char: " + c);
      }
      result = result * 62 + BASE62_INDEX[c];
      if (result < 0) {
        throw new ArithmeticException("Overflow: exceeds Long.MAX_VALUE");
      }
    }
    return result;
  }

  /** Produce a string of repeated characters. */
  private static String repeat(int count) {
    char[] arr = new char[count];
    Arrays.fill(arr, '0');
    return new String(arr);
  }
}
