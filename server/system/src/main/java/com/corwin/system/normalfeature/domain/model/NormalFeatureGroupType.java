package com.corwin.system.normalfeature.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;
import com.corwin.framework.dict.DictTagColor;
import com.corwin.framework.dict.DictTagType;

/**
 *
 * @author Corwin 2026/4/19
 */
public enum NormalFeatureGroupType implements DictEnumDefinition {
    GRAY("灰度组", DictTagColor.PRIMARY_BLUE, DictTagType.INFO),
    WHITELIST("白名单", DictTagColor.SUCCESS_GREEN, DictTagType.SUCCESS),
    PLAN("套餐组", DictTagColor.WARNING_ORANGE, DictTagType.WARNING),
    CHANNEL("渠道组", DictTagColor.SKY, DictTagType.INFO),
    REGION("地区组", DictTagColor.PURPLE, DictTagType.INFO);

    private final String label;
    private final DictTagColor tagColor;
    private final DictTagType tagType;

    NormalFeatureGroupType(String label, DictTagColor tagColor, DictTagType tagType) {
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
