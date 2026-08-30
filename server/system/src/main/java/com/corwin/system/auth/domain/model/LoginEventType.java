package com.corwin.system.auth.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;
import com.corwin.framework.dict.DictTagColor;
import com.corwin.framework.dict.DictTagType;

/**
 * Enum representing the types of login/logout/security events that can be recorded in the audit
 * log.
 *
 * @author Corwin 2026/1/23
 */
public enum LoginEventType implements DictEnumDefinition {
  LOGIN_SUCCESS("成功", DictTagColor.SUCCESS_GREEN, DictTagType.SUCCESS),
  LOGIN_FAILURE("失败", DictTagColor.DANGER_RED, DictTagType.DANGER),
  LOGOUT("登出", DictTagColor.SLATE, DictTagType.INFO),
  TOKEN_EXPIRED("过期", DictTagColor.SLATE, DictTagType.INFO),
  TOKEN_REVOKED("撤销", DictTagColor.SLATE, DictTagType.INFO),
  KICKED_OUT_BY_NEW_LOGIN("挤出", DictTagColor.SLATE, DictTagType.INFO),
  KICKED_OUT_BY_ADMIN("提出", DictTagColor.SLATE, DictTagType.INFO),
  PASSWORD_RESET_REVOKED("重置", DictTagColor.SLATE, DictTagType.INFO),
  USER_DISABLED_REVOKED("禁用", DictTagColor.SLATE, DictTagType.INFO),
  ;

  private final String label;
  private final DictTagColor tagColor;
  private final DictTagType tagType;

  LoginEventType(String label, DictTagColor tagColor, DictTagType tagType) {
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
