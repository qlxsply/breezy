package com.corwin.system.userfeature.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;

/**
 * 产品功能类型。
 *
 * <p>用于区分应用内功能的控制对象类型。</p>
 *
 * @author Corwin
 */
public enum ProductFeatureType implements DictEnumDefinition {
    /**
     * 页面入口。
     */
    PAGE_ENTRY("页面入口"),
    /**
     * 子功能。
     */
    SUB_FEATURE("子功能"),
    /**
     * 按钮。
     */
    BUTTON("按钮"),
    /**
     * 操作。
     */
    OPERATION("操作"),
    ;

    private final String label;

    ProductFeatureType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
