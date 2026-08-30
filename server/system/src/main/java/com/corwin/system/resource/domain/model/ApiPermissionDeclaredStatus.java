package com.corwin.system.resource.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;
import com.corwin.framework.dict.DictTagColor;
import com.corwin.framework.dict.DictTagType;

/**
 * API permission declaration status enumeration.
 *
 * <p>Indicates whether an API endpoint has a permission declaration (DECLARED) or does not
 * (UNDECLARED).
 *
 * @author Corwin 2026/6/21
 */
public enum ApiPermissionDeclaredStatus implements DictEnumDefinition {
  DECLARED("已声明", DictTagColor.SUCCESS_GREEN, DictTagType.SUCCESS),
  UNDECLARED("未声明", DictTagColor.WARNING_ORANGE, DictTagType.WARNING);

  private final String label;
  private final DictTagColor tagColor;
  private final DictTagType tagType;

  ApiPermissionDeclaredStatus(String label, DictTagColor tagColor, DictTagType tagType) {
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
