package com.corwin.system.userfeature.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;

/**
 * 用户访问特例类型。
 *
 * <p>用于表达用户维度的单独启用、单独禁用或清除特例。</p>
 *
 * @author Corwin
 */
public enum UserAccessOverrideType implements DictEnumDefinition {
    /**
     * 无特例。
     */
    NONE("无特例"),
    /**
     * 单独启用。
     */
    ENABLE("单独启用"),
    /**
     * 单独禁用。
     */
    DISABLE("单独禁用"),
    ;

    private final String label;

    UserAccessOverrideType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
