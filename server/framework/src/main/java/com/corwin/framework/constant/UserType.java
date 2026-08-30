package com.corwin.framework.constant;

import com.corwin.framework.dict.DictEnumDefinition;
import com.corwin.framework.dict.DictTagColor;
import com.corwin.framework.dict.DictTagType;

/**
 * Built-in user type enumeration.
 *
 * <p>Defines the hierarchy of system identities with corresponding tag colors and tag types for UI
 * display.
 *
 * @author Corwin 2026/4/19
 */
public enum UserType implements DictEnumDefinition {
  SYSTEM("系统账号", DictTagColor.WARNING_ORANGE, DictTagType.WARNING),
  ADMIN("账号", DictTagColor.PRIMARY_BLUE, DictTagType.INFO),
  USER("用户", DictTagColor.SLATE, DictTagType.INFO),
  GUEST("游客", DictTagColor.DANGER_RED, DictTagType.DANGER);

  private final String label;
  private final DictTagColor tagColor;
  private final DictTagType tagType;

  UserType(String label, DictTagColor tagColor, DictTagType tagType) {
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
