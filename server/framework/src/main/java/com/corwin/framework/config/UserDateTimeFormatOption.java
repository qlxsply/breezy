package com.corwin.framework.config;

import com.corwin.framework.dict.DictEnumDefinition;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * User-selectable date-time format options.
 *
 * @author Corwin 2026/5/5
 */
public enum UserDateTimeFormatOption implements DictEnumDefinition {
  YMD_DASH_24H_MINUTE("年-月-日 24小时制", "yyyy-MM-dd HH:mm"),
  YMD_DASH_24H_SECOND("年-月-日 24小时制（精确到秒）", "yyyy-MM-dd HH:mm:ss"),
  YMD_CHINESE_24H_MINUTE("中文年月日 24小时制", "yyyy年M月d日 HH:mm"),
  YMD_CHINESE_24H_SECOND("中文年月日 24小时制（精确到秒）", "yyyy年M月d日 HH:mm:ss"),
  YMD_SLASH_24H_MINUTE("年/月/日 24小时制", "yyyy/MM/dd HH:mm"),
  YMD_SLASH_24H_SECOND("年/月/日 24小时制（精确到秒）", "yyyy/MM/dd HH:mm:ss"),
  ISO_8601_OFFSET_MINUTE("ISO 8601 包含时区", "yyyy-MM-dd'T'HH:mmXXX"),
  ISO_8601_OFFSET_SECOND("ISO 8601 包含时区（精确到秒）", "yyyy-MM-dd'T'HH:mm:ssXXX"),
  DMY_SLASH_24H_MINUTE("日/月/年 24小时制", "dd/MM/yyyy HH:mm"),
  DMY_SLASH_24H_SECOND("日/月/年 24小时制（精确到秒）", "dd/MM/yyyy HH:mm:ss"),
  MDY_SLASH_12H_MINUTE("月/日/年 12小时制", "MM/dd/yyyy h:mm a"),
  MDY_SLASH_12H_SECOND("月/日/年 12小时制（精确到秒）", "MM/dd/yyyy h:mm:ss a"),
  ENGLISH_MONTH_12H_MINUTE("英文月份 12小时制", "MMM d, yyyy h:mm a"),
  ENGLISH_MONTH_12H_SECOND("英文月份 12小时制（精确到秒）", "MMM d, yyyy h:mm:ss a"),
  COMPACT_24H_MINUTE("紧凑格式", "yyyyMMdd HHmm"),
  COMPACT_24H_SECOND("紧凑格式（精确到秒）", "yyyyMMdd HHmmss");

  private final String label;
  private final String pattern;

  UserDateTimeFormatOption(String label, String pattern) {
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
  public static UserDateTimeFormatOption fromCode(String code) {
    if (code == null || code.isBlank()) {
      return YMD_DASH_24H_SECOND;
    }
    String normalized = code.trim();
    for (UserDateTimeFormatOption option : values()) {
      if (option.itemValue().equals(normalized)) {
        return option;
      }
    }
    return YMD_DASH_24H_SECOND;
  }

  public static String patternOf(String code) {
    return fromCode(code).pattern();
  }
}
