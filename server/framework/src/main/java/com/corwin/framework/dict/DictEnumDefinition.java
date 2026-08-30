package com.corwin.framework.dict;

/**
 * Base interface for dictionary-style enums used throughout the system.
 *
 * <p>Provides default implementations for {@link #itemCode()} (enum name), {@link #itemValue()}
 * (alias for code), alongside optional {@link #tagColor()} and {@link #tagType()} for UI tag
 * rendering.
 *
 * @author Corwin 2026/3/15
 */
public interface DictEnumDefinition {

  default String itemCode() {
    return ((Enum<?>) this).name();
  }

  default String itemValue() {
    return itemCode();
  }

  String label();

  default String tagColor() {
    return "";
  }

  default String tagType() {
    return "";
  }
}
