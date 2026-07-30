package com.corwin.framework.dict;

/**
 * Predefined tag type categories for UI badges and indicators.
 * <p>
 * Each constant represents a semantic category (info, success, warning, danger)
 * used to style tags, alerts, and status markers consistently.
 *
 * @author Corwin 2026/3/15
 */
public enum DictTagType implements DictEnumDefinition {
    INFO("信息", "info"),
    SUCCESS("成功", "success"),
    WARNING("警告", "warning"),
    DANGER("危险", "danger"),
    ;

    private final String label;
    private final String value;

    DictTagType(String label, String value) {
        this.label = label;
        this.value = value;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public String itemValue() {
        return value;
    }

    @Override
    public String tagType() {
        return value;
    }
}
