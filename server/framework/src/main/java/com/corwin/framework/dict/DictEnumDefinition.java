package com.corwin.framework.dict;

/**
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
