package com.corwin.system.resource.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;
import com.corwin.framework.dict.DictTagColor;
import com.corwin.framework.dict.DictTagType;

/**
 * API audit declaration status enumeration.
 *
 * <p>Indicates whether audit logging is enabled (ENABLED) or disabled (DISABLED) for an API
 * endpoint.
 *
 * @author Corwin 2026/6/21
 */
public enum ApiAuditDeclaredStatus implements DictEnumDefinition {
  ENABLED("已开启", DictTagColor.SUCCESS_GREEN, DictTagType.SUCCESS),
  DISABLED("未开启", DictTagColor.SLATE, DictTagType.INFO);

  private final String label;
  private final DictTagColor tagColor;
  private final DictTagType tagType;

  ApiAuditDeclaredStatus(String label, DictTagColor tagColor, DictTagType tagType) {
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
