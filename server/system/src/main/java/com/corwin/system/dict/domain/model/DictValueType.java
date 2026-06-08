package com.corwin.system.dict.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;
import com.corwin.framework.dict.DictTagColor;
import com.corwin.framework.dict.DictTagType;

/**
 * @author Corwin 2026/3/15
 */
public enum DictValueType implements DictEnumDefinition {
    STRING("文本", DictTagColor.PRIMARY_BLUE, DictTagType.INFO),
    NUMBER("数字", DictTagColor.WARNING_ORANGE, DictTagType.WARNING),
    BOOLEAN("布尔", DictTagColor.SUCCESS_GREEN, DictTagType.SUCCESS);

    private final String label;
    private final DictTagColor tagColor;
    private final DictTagType tagType;

    DictValueType(String label, DictTagColor tagColor, DictTagType tagType) {
        this.label = label;
        this.tagColor = tagColor;
        this.tagType = tagType;
    }

    @Override
    public String label() { return label; }

    @Override
    public String tagColor() { return tagColor.itemValue(); }

    @Override
    public String tagType() { return tagType.itemValue(); }
}
