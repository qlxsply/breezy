package com.corwin.framework.web.auth;

import com.corwin.framework.dict.DictEnumDefinition;
import lombok.Getter;

/**
 * Whitelist matching strategy types.
 * <p>
 * Each enum constant represents a different path-matching approach used
 * to determine whether a request URI is exempt from authentication.
 *
 * @author Corwin 2026/3/23
 */
@Getter
public enum WhitelistMatchType implements DictEnumDefinition {
    EXACT("精确匹配"),
    ANT("Ant 路径匹配"),
    PATH_PATTERN("PathPattern 匹配");

    private final String label;

    WhitelistMatchType(String label) {
        this.label = label;
    }

    @Override
    public String itemValue() {
        return this.name();
    }

    @Override
    public String label() {
        return this.label;
    }

    public static WhitelistMatchType fromCode(String code) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("whitelist type cannot be blank");
        }
        if (WhitelistMatchType.EXACT.name().equals(code)) {
            return WhitelistMatchType.EXACT;
        } else if (WhitelistMatchType.ANT.name().equals(code)) {
            return WhitelistMatchType.ANT;
        } else if (WhitelistMatchType.PATH_PATTERN.name().equals(code)) {
            return WhitelistMatchType.PATH_PATTERN;
        }
        throw new IllegalArgumentException("unsupported whitelist type: " + code);
    }

}
