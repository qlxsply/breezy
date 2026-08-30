package com.corwin.reminder.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;
import com.corwin.framework.dict.DictTagColor;
import com.corwin.framework.dict.DictTagType;

/**
 * @author Corwin 2026/2/4
 */
public enum ReminderOutboxStatus implements DictEnumDefinition {
  PENDING("待发送", DictTagColor.SLATE, DictTagType.INFO),
  SENT("已发送", DictTagColor.PRIMARY_BLUE, DictTagType.INFO),
  ACKED("已确认", DictTagColor.SUCCESS_GREEN, DictTagType.SUCCESS),
  CANCELED("已取消", DictTagColor.WARNING_ORANGE, DictTagType.WARNING);

  private final String label;
  private final DictTagColor tagColor;
  private final DictTagType tagType;

  ReminderOutboxStatus(String label, DictTagColor tagColor, DictTagType tagType) {
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
