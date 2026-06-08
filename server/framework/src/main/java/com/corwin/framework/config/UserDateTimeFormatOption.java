package com.corwin.framework.config;

import com.corwin.framework.dict.DictEnumDefinition;

/**
 * @author Corwin 2026/5/5
 */
public enum UserDateTimeFormatOption implements DictEnumDefinition {
    YYYY_MM_DD_HH_MM_SS("年-月-日 24小时", "yyyy-MM-dd HH:mm:ss"),
    YYYY_SLASH_MM_DD_HH_MM_SS("年/月/日 24小时", "yyyy/MM/dd HH:mm:ss"),
    DD_SLASH_MM_YYYY_HH_MM_SS("日/月/年 24小时", "dd/MM/yyyy HH:mm:ss"),
    MM_DD_YYYY_HH_MM("月-日-年 24小时", "MM-dd-yyyy HH:mm");

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
    public String itemValue() {
        return pattern;
    }

    public String pattern() {
        return pattern;
    }

    public static UserDateTimeFormatOption fromCode(String code) {
        if (code == null || code.isBlank()) {
            return YYYY_MM_DD_HH_MM_SS;
        }
        String normalized = code.trim();
        for (UserDateTimeFormatOption option : values()) {
            if (option.name().equals(normalized)) {
                return option;
            }
        }
        return YYYY_MM_DD_HH_MM_SS;
    }

    public static String patternOf(String code) {
        return fromCode(code).pattern();
    }
}
