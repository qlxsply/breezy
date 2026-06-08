package com.corwin.system.normalfeature.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;
import com.corwin.framework.dict.DictTagColor;
import com.corwin.framework.dict.DictTagType;

/**
 *
 * @author Corwin 2026/4/19
 */
public enum NormalFeatureOverrideType implements DictEnumDefinition {
    NONE("无覆盖", DictTagColor.SLATE, DictTagType.INFO),
    ENABLE("强制开启", DictTagColor.SUCCESS_GREEN, DictTagType.SUCCESS),
    DISABLE("强制关闭", DictTagColor.DANGER_RED, DictTagType.DANGER);

    private final String label;
    private final DictTagColor tagColor;
    private final DictTagType tagType;

    NormalFeatureOverrideType(String label, DictTagColor tagColor, DictTagType tagType) {
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
