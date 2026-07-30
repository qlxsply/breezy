package com.corwin.framework.dict;

/**
 * Predefined tag color palette with corresponding hex values and tag types.
 * <p>
 * Used to render consistent colored badges and indicators in the UI.
 * Each constant maps a semantic color name to a concrete hex value.
 *
 * @author Corwin 2026/3/15
 */
public enum DictTagColor implements DictEnumDefinition {
    PRIMARY_BLUE("主题蓝", "#3B82F6", DictTagType.INFO),
    SUCCESS_GREEN("成功绿", "#10B981", DictTagType.SUCCESS),
    WARNING_ORANGE("警示橙", "#F59E0B", DictTagType.WARNING),
    DANGER_RED("危险红", "#EF4444", DictTagType.DANGER),
    SLATE("石板灰", "#64748B", DictTagType.INFO),
    PURPLE("紫罗兰", "#8B5CF6", DictTagType.INFO),
    SKY("天空蓝", "#0EA5E9", DictTagType.INFO),
    STEEL("钢灰", "#94A3B8", DictTagType.INFO),
    ;

    private final String label;
    private final String value;
    private final DictTagType tagType;

    DictTagColor(String label, String value, DictTagType tagType) {
        this.label = label;
        this.value = value;
        this.tagType = tagType;
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
    public String tagColor() {
        return value;
    }

    @Override
    public String tagType() {
        return tagType.itemValue();
    }
}
