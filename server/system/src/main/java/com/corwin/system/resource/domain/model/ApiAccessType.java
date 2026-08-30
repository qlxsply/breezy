package com.corwin.system.resource.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;
import com.corwin.framework.dict.DictTagColor;
import com.corwin.framework.dict.DictTagType;

/**
 * API access type enumeration.
 *
 * <p>Defines the access control level for an API endpoint: public access (PERMIT_ALL),
 * authenticated-only (AUTHENTICATED), authorized with permissions (AUTHORIZED), or completely
 * denied (DENY).
 *
 * @author Corwin 2026/4/19
 */
public enum ApiAccessType implements DictEnumDefinition {
  PERMIT_ALL("公开访问", DictTagColor.SUCCESS_GREEN, DictTagType.SUCCESS),
  AUTHENTICATED("已认证", DictTagColor.PRIMARY_BLUE, DictTagType.INFO),
  AUTHORIZED("已授权", DictTagColor.WARNING_ORANGE, DictTagType.WARNING),
  DENY("拒绝访问", DictTagColor.DANGER_RED, DictTagType.DANGER);

  private final String label;
  private final DictTagColor tagColor;
  private final DictTagType tagType;

  ApiAccessType(String label, DictTagColor tagColor, DictTagType tagType) {
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
