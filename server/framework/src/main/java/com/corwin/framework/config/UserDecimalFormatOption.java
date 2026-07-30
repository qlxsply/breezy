package com.corwin.framework.config;

import com.corwin.framework.dict.DictEnumDefinition;

/**
 * User-selectable decimal number format options.
 *
 * @author Corwin 2026/5/5
 */
public enum UserDecimalFormatOption implements DictEnumDefinition {
    COMMA_2("千分位，两位小数", "#,##0.00"),
    COMMA_3("千分位，三位小数", "#,##0.000"),
    PLAIN_2("无千分位，两位小数", "0.00"),
    PLAIN_3("无千分位，三位小数", "0.000"),
    PLAIN_4("无千分位，四位小数", "0.0000");

    private final String label;
    private final String pattern;

    UserDecimalFormatOption(String label, String pattern) {
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

    public static UserDecimalFormatOption fromCode(String code) {
        if (code == null || code.isBlank()) {
            return COMMA_2;
        }
        String normalized = code.trim();
        for (UserDecimalFormatOption option : values()) {
            if (option.name().equals(normalized)) {
                return option;
            }
        }
        return COMMA_2;
    }

    public static String patternOf(String code) {
        return fromCode(code).pattern();
    }
}
