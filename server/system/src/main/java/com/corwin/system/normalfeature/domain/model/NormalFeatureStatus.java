package com.corwin.system.normalfeature.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;
import com.corwin.framework.dict.DictTagColor;
import com.corwin.framework.dict.DictTagType;

/**
 *
 * @author Corwin 2026/4/19
 */
public enum NormalFeatureStatus implements DictEnumDefinition {
    DRAFT("草稿", DictTagColor.SLATE, DictTagType.INFO),
    PUBLISHED("已发布", DictTagColor.SUCCESS_GREEN, DictTagType.SUCCESS),
    SUSPENDED("已停用", DictTagColor.WARNING_ORANGE, DictTagType.WARNING),
    DEPRECATED("已废弃", DictTagColor.DANGER_RED, DictTagType.DANGER);

    private final String label;
    private final DictTagColor tagColor;
    private final DictTagType tagType;

    NormalFeatureStatus(String label, DictTagColor tagColor, DictTagType tagType) {
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
