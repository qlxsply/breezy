package com.corwin.framework.config;

import com.corwin.framework.dict.DictEnumDefinition;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * User-selectable date format options.
 *
 * @author Corwin 2026/5/5
 */
public enum UserDateFormatOption implements DictEnumDefinition {
  YMD_DASH("年-月-日", "yyyy-MM-dd"),
  YMD_SLASH("年/月/日", "yyyy/MM/dd"),
  YMD_CHINESE("中文年月日", "yyyy年M月d日"),
  DMY_SLASH("日/月/年", "dd/MM/yyyy"),
  MDY_SLASH("月/日/年", "MM/dd/yyyy"),
  EN_MONTH_SHORT("英文月份简写", "MMM d, yyyy"),
  EN_DAY_MONTH_SHORT("日 英文月份 年", "d MMM yyyy"),
  COMPACT("紧凑格式", "yyyyMMdd");

  private final String label;
  private final String pattern;

  UserDateFormatOption(String label, String pattern) {
    this.label = label;
    this.pattern = pattern;
  }

  @Override
  public String label() {
    return label;
  }

  @Override
  @JsonValue
  public String itemValue() {
    return pattern;
  }

  public String pattern() {
    return pattern;
  }

  @JsonCreator
  public static UserDateFormatOption fromCode(String code) {
    if (code == null || code.isBlank()) {
      return YMD_DASH;
    }
    String normalized = code.trim();
    for (UserDateFormatOption option : values()) {
      if (option.itemValue().equals(normalized)) {
        return option;
      }
    }
    return YMD_DASH;
  }

  public static String patternOf(String code) {
    return fromCode(code).pattern();
  }
}
