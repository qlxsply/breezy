package com.corwin.framework.config;

import com.corwin.framework.dict.DictEnumDefinition;

/**
 * 用户可选的千分位与小数点组合。
 *
 * <p>该枚举只表达“千分位 + 小数点”的符号组合，不再承载小数位数和舍入模式；
 * 小数位数与舍入模式由系统配置资源 {@code framework.format.decimal-policy} 统一管理。
 *
 * @author Corwin 2026/7/31
 */
public enum UserDecimalFormatOption implements DictEnumDefinition {

    /**
     * 千分位逗号、小数点：1,234.56
     */
    COMMA_DOT("千分位逗号、小数点", ",", "."),
    /**
     * 无千分位、小数点：1234.56
     */
    PLAIN_DOT("无千分位、小数点", "", "."),
    /**
     * 千分位点、逗号小数点：1.234,56
     */
    DOT_COMMA("千分位点、逗号小数点", ".", ",");

    private final String label;
    private final String groupingSeparator;
    private final String decimalSeparator;

    UserDecimalFormatOption(String label, String groupingSeparator, String decimalSeparator) {
        this.label = label;
        this.groupingSeparator = groupingSeparator;
        this.decimalSeparator = decimalSeparator;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public String itemValue() {
        return name();
    }

    public String groupingSeparator() {
        return groupingSeparator;
    }

    public String decimalSeparator() {
        return decimalSeparator;
    }

    public boolean groupingUsed() {
        return !groupingSeparator.isEmpty();
    }

    public static UserDecimalFormatOption fromCode(String code) {
        if (code == null || code.isBlank()) {
            return COMMA_DOT;
        }
        String normalized = code.trim();
        for (UserDecimalFormatOption option : values()) {
            if (option.name().equals(normalized)) {
                return option;
            }
        }
        return COMMA_DOT;
    }
}
