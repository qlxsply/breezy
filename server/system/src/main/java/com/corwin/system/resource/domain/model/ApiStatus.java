package com.corwin.system.resource.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;
import com.corwin.framework.dict.DictTagColor;
import com.corwin.framework.dict.DictTagType;

/**
 * API lifecycle status enumeration.
 *
 * <p>Represents the states an API can be in: draft, active, disabled, or deleted.
 *
 * @author Corwin 2026/1/23
 */
public enum ApiStatus implements DictEnumDefinition {
  DRAFT("草稿", DictTagColor.SLATE, DictTagType.INFO),
  ACTIVE("启用", DictTagColor.SUCCESS_GREEN, DictTagType.SUCCESS),
  DISABLED("停用", DictTagColor.WARNING_ORANGE, DictTagType.WARNING),
  DELETED("删除", DictTagColor.DANGER_RED, DictTagType.DANGER);

  private final String label;
  private final DictTagColor tagColor;
  private final DictTagType tagType;

  ApiStatus(String label, DictTagColor tagColor, DictTagType tagType) {
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
