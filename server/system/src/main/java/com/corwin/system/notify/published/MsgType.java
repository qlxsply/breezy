package com.corwin.system.notify.published;

import com.corwin.framework.dict.DictEnumDefinition;
import com.corwin.framework.dict.DictTagColor;
import com.corwin.framework.dict.DictTagType;

/**
 * @author Corwin 2026/4/16
 */
public enum MsgType implements DictEnumDefinition {
    TODO_REMINDER("待办提醒", DictTagColor.PRIMARY_BLUE, DictTagType.INFO),
    SYSTEM_EVENT("系统事件", DictTagColor.SLATE, DictTagType.INFO),
    BUSINESS_EVENT("业务事件", DictTagColor.SUCCESS_GREEN, DictTagType.SUCCESS);

    private final String label;
    private final DictTagColor tagColor;
    private final DictTagType tagType;

    MsgType(String label, DictTagColor tagColor, DictTagType tagType) {
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
