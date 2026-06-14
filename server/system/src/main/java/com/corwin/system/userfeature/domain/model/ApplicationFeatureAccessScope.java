package com.corwin.system.userfeature.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;

/**
 * 应用功能访问范围。
 *
 * <p>用于定义应用级授权或应用级用户特例覆盖的功能范围。</p>
 *
 * @author Corwin
 */
public enum ApplicationFeatureAccessScope implements DictEnumDefinition {
    /**
     * 完整功能。
     *
     * <p>拥有应用下全部已启用功能，后续新增功能默认可用。</p>
     */
    FULL("全部功能"),
    /**
     * 部分功能。
     *
     * <p>仅拥有显式授权的功能，后续新增功能默认不可用。</p>
     */
    PARTIAL("部分功能"),
    ;

    private final String label;

    ApplicationFeatureAccessScope(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
