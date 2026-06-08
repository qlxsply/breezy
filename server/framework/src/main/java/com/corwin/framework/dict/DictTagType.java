package com.corwin.framework.dict;

/**
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
