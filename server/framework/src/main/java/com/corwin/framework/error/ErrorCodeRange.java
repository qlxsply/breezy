package com.corwin.framework.error;

/**
 * Defines a range of error codes (inclusive boundaries).
 *
 * <p>Codes are fixed-length numeric strings; containment is determined by lexicographic comparison.
 *
 * @author Corwin 2026/3/30
 */
public interface ErrorCodeRange {

  /** Range name for identification (e.g. in error messages). */
  String getName();

  /** Start code (inclusive). */
  String getStart();

  /** End code (inclusive). */
  String getEnd();

  /** Checks whether the given code falls within this range. */
  default boolean contains(String code) {
    if (code == null) {
      return false;
    }
    return code.compareTo(getStart()) >= 0 && code.compareTo(getEnd()) <= 0;
  }
}
