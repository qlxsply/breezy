package com.corwin.system.auth.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;
import com.corwin.framework.dict.DictTagColor;
import com.corwin.framework.dict.DictTagType;

/**
 *
 * @author Corwin 2026/4/19
 */
public enum RefreshTokenStatus implements DictEnumDefinition {
    ACTIVE("有效", DictTagColor.SUCCESS_GREEN, DictTagType.SUCCESS),
    ROTATED("已轮换", DictTagColor.PRIMARY_BLUE, DictTagType.INFO),
    EXPIRED("已过期", DictTagColor.SLATE, DictTagType.INFO),
    REVOKED("已撤销", DictTagColor.DANGER_RED, DictTagType.DANGER);

    private final String label;
    private final DictTagColor tagColor;
    private final DictTagType tagType;

    RefreshTokenStatus(String label, DictTagColor tagColor, DictTagType tagType) {
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
