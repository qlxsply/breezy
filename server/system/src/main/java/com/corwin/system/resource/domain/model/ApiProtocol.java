package com.corwin.system.resource.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;
import com.corwin.framework.dict.DictTagColor;
import com.corwin.framework.dict.DictTagType;

/**
 * API communication protocol enumeration.
 *
 * <p>Currently only HTTP is supported as the transport protocol.</p>
 *
 * @author Corwin 2026/1/23
 */
public enum ApiProtocol implements DictEnumDefinition {
    HTTP("HTTP", DictTagColor.PRIMARY_BLUE, DictTagType.INFO);

    private final String label;
    private final DictTagColor tagColor;
    private final DictTagType tagType;

    ApiProtocol(String label, DictTagColor tagColor, DictTagType tagType) {
        this.label = label;
        this.tagColor = tagColor;
        this.tagType = tagType;
    }

    @Override public String label() { return label; }
    @Override public String tagColor() { return tagColor.itemValue(); }
    @Override public String tagType() { return tagType.itemValue(); }
}
