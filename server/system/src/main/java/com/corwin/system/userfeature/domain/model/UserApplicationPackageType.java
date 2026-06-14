package com.corwin.system.userfeature.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;

/**
 * 用户应用包类型。
 *
 * <p>用于区分用户应用包的业务来源。</p>
 *
 * @author Corwin
 */
public enum UserApplicationPackageType implements DictEnumDefinition {
    /**
     * 默认包。
     */
    DEFAULT("默认包"),
    /**
     * 会员包。
     */
    MEMBERSHIP("会员包"),
    /**
     * 运营包。
     */
    OPERATION("运营包"),
    /**
     * 企业包。
     */
    ENTERPRISE("企业包"),
    /**
     * 自定义包。
     */
    CUSTOM("自定义包"),
    ;

    private final String label;

    UserApplicationPackageType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
