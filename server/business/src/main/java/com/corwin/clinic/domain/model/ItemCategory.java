package com.corwin.clinic.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;
import com.corwin.framework.dict.DictTagColor;
import com.corwin.framework.dict.DictTagType;

/**
 * @author Corwin 2026/2/8
 */
public enum ItemCategory implements DictEnumDefinition {
    DRUG("药品", DictTagColor.PRIMARY_BLUE, DictTagType.INFO),
    CONSUMABLE("耗材", DictTagColor.SUCCESS_GREEN, DictTagType.SUCCESS),
    EQUIPMENT("设备", DictTagColor.WARNING_ORANGE, DictTagType.WARNING);

    private final String label;
    private final DictTagColor tagColor;
    private final DictTagType tagType;

    ItemCategory(String label, DictTagColor tagColor, DictTagType tagType) {
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
