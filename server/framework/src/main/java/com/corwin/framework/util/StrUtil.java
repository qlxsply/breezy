package com.corwin.framework.util;

import com.corwin.framework.constant.TextConstants;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Null-safe string utility methods.
 *
 * <p>Provides common operations such as emptiness/blankness checks, joining, splitting, trimming,
 * and default-value fallbacks, complementing the standard {@link java.lang.String} API.
 *
 * @author Corwin 2026/1/11
 */
public final class StrUtil {

  private StrUtil() {}

  /**
   * Checks whether the given string is {@code null} or empty ({@code ""}).
   *
   * @param s the string to check, may be null
   * @return {@code true} if null or empty
   */
  public static boolean isEmpty(String s) {
    return s == null || s.isEmpty();
  }

  /**
   * Checks whether the given string is {@code null} or contains only whitespace characters.
   *
   * @param s the string to check, may be null
   * @return {@code true} if null, empty, or blank
   */
  public static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

  /**
   * Returns the inverse of {@link #isEmpty(String)}.
   *
   * @param s the string to check, may be null
   * @return {@code true} if not null and not empty
   */
  public static boolean isNotEmpty(String s) {
    return !isEmpty(s);
  }

  /**
   * Returns the inverse of {@link #isBlank(String)}.
   *
   * @param s the string to check, may be null
   * @return {@code true} if not null and contains non-whitespace
   */
  public static boolean isNotBlank(String s) {
    return !isBlank(s);
  }

  /**
   * Returns {@code ""} if the input is {@code null}, otherwise the input unchanged.
   *
   * @param s the string to convert, may be null
   * @return the original string or {@code ""} if null
   */
  public static String nullToEmpty(String s) {
    return s == null ? TextConstants.EMPTY : s;
  }

  /**
   * Returns {@code null} if the input is {@code null} or empty.
   *
   * @param s the string to convert, may be null
   * @return {@code null} if null or empty, otherwise the original string
   */
  public static String emptyToNull(String s) {
    return (s == null || s.isEmpty()) ? null : s;
  }

  /**
   * Trims the input; returns {@code ""} if the input is {@code null}.
   *
   * @param s the string to trim, may be null
   * @return the trimmed string, or {@code ""} if null
   */
  public static String trimToEmpty(String s) {
    return s == null ? TextConstants.EMPTY : s.trim();
  }

  /**
   * Trims the input; returns {@code null} if the result is empty.
   *
   * @param s the string to trim, may be null
   * @return the trimmed string, or {@code null} if the trimmed result is empty
   */
  public static String trimToNull(String s) {
    if (s == null) {
      return null;
    }
    String trimmed = s.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  /**
   * Returns the input if non-null, otherwise the given default value.
   *
   * @param s the string to evaluate, may be null
   * @param defaultVal the fallback value
   * @return the original string or {@code defaultVal} if null
   */
  public static String defaultIfNull(String s, String defaultVal) {
    return s == null ? defaultVal : s;
  }

  /**
   * Returns the input if not blank, otherwise the given default value.
   *
   * @param s the string to evaluate, may be null
   * @param defaultVal the fallback value
   * @return the original string or {@code defaultVal} if null, empty, or blank
   */
  public static String defaultIfBlank(String s, String defaultVal) {
    return isBlank(s) ? defaultVal : s;
  }

  /**
   * Null-safe equality check via {@link Objects#equals(Object, Object)}.
   *
   * @param a the first string, may be null
   * @param b the second string, may be null
   * @return {@code true} if both are equal or both null
   */
  public static boolean equals(String a, String b) {
    return Objects.equals(a, b);
  }

  /**
   * Null-safe case-insensitive equality check.
   *
   * @param a the first string, may be null
   * @param b the second string, may be null
   * @return {@code true} if both are equal ignoring case, or both null
   */
  public static boolean equalsIgnoreCase(String a, String b) {
    if (a == null) {
      return b == null;
    }
    return a.equalsIgnoreCase(b);
  }

  /**
   * Checks whether {@code s} contains {@code search}; handles null safely.
   *
   * @param s the string to search in, may be null
   * @param search the substring to look for, may be null
   * @return {@code true} if {@code search} is found within {@code s}
   */
  public static boolean contains(String s, String search) {
    return s != null && search != null && s.contains(search);
  }

  /**
   * Joins the given items with the default delimiter ({@code ,}).
   *
   * @param items the strings to join
   * @return the joined string, or {@code ""} if the array is empty
   */
  public static String join(String... items) {
    return join(TextConstants.COMMA, items);
  }

  /**
   * Joins the given items with the specified delimiter.
   *
   * @param delimiter the delimiter placed between items
   * @param items the strings to join
   * @return the joined string, or {@code ""} if the array is empty
   * @throws NullPointerException if delimiter or items is null
   */
  public static String join(String delimiter, String... items) {
    Objects.requireNonNull(delimiter, "delimiter must not be null");
    Objects.requireNonNull(items, "items array must not be null");
    if (items.length == 0) {
      return TextConstants.EMPTY;
    }
    return String.join(delimiter, items);
  }

  /**
   * Joins a collection by applying a mapper function, using the default delimiter ({@code ,}).
   *
   * @param <T> the element type
   * @param items the collection to join
   * @param mapper the function to convert each element to a string
   * @return the joined string, or {@code ""} if the collection is empty
   * @throws NullPointerException if items or mapper is null
   */
  public static <T> String join(Collection<T> items, Function<T, String> mapper) {
    return join(items, mapper, TextConstants.COMMA);
  }

  /**
   * Joins a collection by applying a mapper function with the specified delimiter.
   *
   * @param <T> the element type
   * @param items the collection to join
   * @param mapper the function to convert each element to a string
   * @param delimiter the delimiter placed between items
   * @return the joined string, or {@code ""} if the collection is empty
   * @throws NullPointerException if items, mapper, or delimiter is null
   */
  public static <T> String join(Collection<T> items, Function<T, String> mapper, String delimiter) {
    Objects.requireNonNull(items, "items collection must not be null");
    Objects.requireNonNull(mapper, "mapper function must not be null");
    Objects.requireNonNull(delimiter, "delimiter must not be null");
    if (items.isEmpty()) {
      return TextConstants.EMPTY;
    }
    return items.stream()
        .map(item -> Objects.toString(mapper.apply(item), TextConstants.EMPTY))
        .collect(Collectors.joining(delimiter));
  }

  /**
   * Splits the given string by the default delimiter ({@code ,}), trimming each part.
   *
   * @param str the string to split
   * @return an array of trimmed parts, never null
   * @throws NullPointerException if str is null
   */
  public static String[] split(String str) {
    return split(str, TextConstants.COMMA);
  }

  /**
   * Splits the given string by the specified literal delimiter, trimming each part.
   *
   * <p>Uses {@link Pattern#quote(String)} to treat the delimiter as a literal string (not a regex),
   * and a negative limit to preserve trailing empty strings.
   *
   * @param str the string to split
   * @param delimiter the literal delimiter
   * @return an array of trimmed parts, never null
   * @throws NullPointerException if str or delimiter is null
   */
  public static String[] split(String str, String delimiter) {
    Objects.requireNonNull(str, "input string must not be null");
    Objects.requireNonNull(delimiter, "delimiter must not be null");
    if (str.isEmpty()) {
      return new String[] {};
    }
    String[] parts = str.split(Pattern.quote(delimiter), -1);
    for (int i = 0; i < parts.length; i++) {
      parts[i] = parts[i].trim();
    }
    return parts;
  }

  /**
   * Returns the element at the given index after splitting by the default delimiter ({@code ,}), or
   * {@code null} if out of bounds.
   *
   * @param str the string to split
   * @param index the zero-based index of the desired part
   * @return the part at the given index, or {@code null} if out of bounds
   */
  public static String getAtOrNull(String str, int index) {
    return getAtOrNull(str, TextConstants.COMMA, index);
  }

  /**
   * Returns the element at the given index after splitting by the specified delimiter, or {@code
   * null} if out of bounds.
   *
   * @param str the string to split
   * @param delimiter the literal delimiter
   * @param index the zero-based index of the desired part
   * @return the part at the given index, or {@code null} if out of bounds
   */
  public static String getAtOrNull(String str, String delimiter, int index) {
    String[] parts = split(str, delimiter);
    return (index >= 0 && index < parts.length) ? parts[index] : null;
  }
}
