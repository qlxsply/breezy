package com.corwin.system.user.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;
import com.corwin.framework.dict.DictTagColor;
import com.corwin.framework.dict.DictTagType;

/**
 * @author Corwin 2026/1/22
 */
public enum UserStatus implements DictEnumDefinition {
    ENABLED("启用", DictTagColor.SUCCESS_GREEN, DictTagType.SUCCESS),
    DISABLED("禁用", DictTagColor.DANGER_RED, DictTagType.DANGER);

    private final String label;
    private final DictTagColor tagColor;
    private final DictTagType tagType;

    UserStatus(String label, DictTagColor tagColor, DictTagType tagType) {
        this.label = label;
        this.tagColor = tagColor;
        this.tagType = tagType;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public String tagColor() { return tagColor.itemValue(); }

    @Override
    public String tagType() { return tagType.itemValue(); }
}
