package com.corwin.reminder.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;
import com.corwin.framework.dict.DictTagColor;
import com.corwin.framework.dict.DictTagType;

/**
 * @author Corwin 2026/3/12
 */
public enum TodoAttachmentStage implements DictEnumDefinition {
  CONTENT("内容附件", DictTagColor.PRIMARY_BLUE, DictTagType.INFO),
  COMPLETION("完成附件", DictTagColor.SUCCESS_GREEN, DictTagType.SUCCESS);

  private final String label;
  private final DictTagColor tagColor;
  private final DictTagType tagType;

  TodoAttachmentStage(String label, DictTagColor tagColor, DictTagType tagType) {
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
