package com.corwin.datasource.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;
import com.corwin.framework.dict.DictTagColor;
import com.corwin.framework.dict.DictTagType;

/**
 * @author Corwin 2026/1/11
 */
public enum DatabaseSourceStatus implements DictEnumDefinition {
  NEW("未测试", DictTagColor.SLATE, DictTagType.INFO),
  OK("正常", DictTagColor.SUCCESS_GREEN, DictTagType.SUCCESS),
  FAILED("失败", DictTagColor.DANGER_RED, DictTagType.DANGER);

  private final String label;
  private final DictTagColor tagColor;
  private final DictTagType tagType;

  DatabaseSourceStatus(String label, DictTagColor tagColor, DictTagType tagType) {
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
