package com.corwin.system.resource.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;
import com.corwin.framework.dict.DictTagColor;
import com.corwin.framework.dict.DictTagType;

/**
 * @author Corwin 2026/1/23
 */
public enum ApiMethod implements DictEnumDefinition {
    GET("GET", DictTagColor.PRIMARY_BLUE, DictTagType.INFO),
    POST("POST", DictTagColor.SUCCESS_GREEN, DictTagType.SUCCESS),
    PUT("PUT", DictTagColor.WARNING_ORANGE, DictTagType.WARNING),
    DELETE("DELETE", DictTagColor.DANGER_RED, DictTagType.DANGER),
    PATCH("PATCH", DictTagColor.PURPLE, DictTagType.INFO),
    OPTIONS("OPTIONS", DictTagColor.SLATE, DictTagType.INFO),
    HEAD("HEAD", DictTagColor.STEEL, DictTagType.INFO),
    ANY("ANY", DictTagColor.SKY, DictTagType.INFO);

    private final String label;
    private final DictTagColor tagColor;
    private final DictTagType tagType;

    ApiMethod(String label, DictTagColor tagColor, DictTagType tagType) {
        this.label = label;
        this.tagColor = tagColor;
        this.tagType = tagType;
    }

    @Override public String label() { return label; }
    @Override public String tagColor() { return tagColor.itemValue(); }
    @Override public String tagType() { return tagType.itemValue(); }
}
