package com.corwin.framework.config;

import com.corwin.framework.dict.DictEnumDefinition;

/**
 * @author Corwin 2026/5/5
 */
public enum UserDateFormatOption implements DictEnumDefinition {
    YYYY_MM_DD("年-月-日", "yyyy-MM-dd"),
    YYYY_SLASH_MM_DD("年/月/日", "yyyy/MM/dd"),
    DD_SLASH_MM_YYYY("日/月/年", "dd/MM/yyyy"),
    MM_DD_YYYY("月-日-年", "MM-dd-yyyy");

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
    public String itemValue() {
        return pattern;
    }

    public String pattern() {
        return pattern;
    }

    public static UserDateFormatOption fromCode(String code) {
        if (code == null || code.isBlank()) {
            return YYYY_MM_DD;
        }
        String normalized = code.trim();
        for (UserDateFormatOption option : values()) {
            if (option.name().equals(normalized)) {
                return option;
            }
        }
        return YYYY_MM_DD;
    }

    public static String patternOf(String code) {
        return fromCode(code).pattern();
    }
}
