package com.corwin.reminder.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;
import com.corwin.framework.dict.DictTagColor;
import com.corwin.framework.dict.DictTagType;

/**
 *
 * @author Corwin 2026/1/12
 */
public enum RecurrenceRuleFrequency implements DictEnumDefinition {
    ONCE("一次", DictTagColor.SLATE, DictTagType.INFO),
    DAILY("每天", DictTagColor.PRIMARY_BLUE, DictTagType.INFO),
    WEEKLY("每周", DictTagColor.SUCCESS_GREEN, DictTagType.SUCCESS),
    MONTHLY("每月", DictTagColor.WARNING_ORANGE, DictTagType.WARNING),
    YEARLY("每年", DictTagColor.PURPLE, DictTagType.INFO);

    private final String label;
    private final DictTagColor tagColor;
    private final DictTagType tagType;

    RecurrenceRuleFrequency(String label, DictTagColor tagColor, DictTagType tagType) {
        this.label = label;
        this.tagColor = tagColor;
        this.tagType = tagType;
    }

    @Override public String label() { return label; }
    @Override public String tagColor() { return tagColor.itemValue(); }
    @Override public String tagType() { return tagType.itemValue(); }
}
