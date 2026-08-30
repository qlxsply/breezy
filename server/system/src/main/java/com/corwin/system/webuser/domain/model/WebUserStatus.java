package com.corwin.system.webuser.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;
import com.corwin.framework.dict.DictTagColor;
import com.corwin.framework.dict.DictTagType;

/**
 * Account status for a web user. Implements DictEnumDefinition for dictionary tag display.
 *
 * @author Corwin 2026/5/11
 */
public enum WebUserStatus implements DictEnumDefinition {
  ACTIVE("启用", DictTagColor.SUCCESS_GREEN, DictTagType.SUCCESS),
  DISABLED("停用", DictTagColor.WARNING_ORANGE, DictTagType.WARNING),
  CANCELLED("已注销", DictTagColor.DANGER_RED, DictTagType.DANGER);

  private final String label;
  private final DictTagColor tagColor;
  private final DictTagType tagType;

  WebUserStatus(String label, DictTagColor tagColor, DictTagType tagType) {
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
