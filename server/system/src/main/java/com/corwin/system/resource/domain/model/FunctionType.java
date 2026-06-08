package com.corwin.system.resource.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;
import com.corwin.framework.dict.DictTagColor;
import com.corwin.framework.dict.DictTagType;

/**
 *
 * @author Corwin 2026/4/19
 */
public enum FunctionType implements DictEnumDefinition {
    PAGE("页面", DictTagColor.PRIMARY_BLUE, DictTagType.INFO),
    QUERY("查询", DictTagColor.SUCCESS_GREEN, DictTagType.SUCCESS),
    ACTION("操作", DictTagColor.WARNING_ORANGE, DictTagType.WARNING),
    BUTTON("按钮", DictTagColor.SKY, DictTagType.INFO),
    INVISIBLE("隐藏能力", DictTagColor.SLATE, DictTagType.INFO);

    private final String label;
    private final DictTagColor tagColor;
    private final DictTagType tagType;

    FunctionType(String label, DictTagColor tagColor, DictTagType tagType) {
        this.label = label;
        this.tagColor = tagColor;
        this.tagType = tagType;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public String tagColor() {
        return tagColor.itemValue();
    }

    @Override
    public String tagType() {
        return tagType.itemValue();
    }
}
