package com.corwin.framework.util;

import com.corwin.framework.dict.DictEnumDefinition;
import com.corwin.framework.dict.DictTagColor;
import com.corwin.framework.dict.DictTagType;

/**
 * 客户端 IP 解析模式。
 *
 * @author Corwin 2026/3/21
 */
public enum ClientIpMode implements DictEnumDefinition {
    REMOTE_ADDR("REMOTE_ADDR（直连地址）", DictTagColor.PRIMARY_BLUE, DictTagType.INFO),
    X_REAL_IP("X-Real-IP", DictTagColor.SLATE, DictTagType.INFO),
    X_FORWARDED_FOR_FIRST("X-Forwarded-For（取首个）", DictTagColor.SLATE, DictTagType.INFO),
    X_FORWARDED_FOR_LAST("X-Forwarded-For（取最后）", DictTagColor.SLATE, DictTagType.INFO),
    CF_Connecting_IP("CF-Connecting-IP", DictTagColor.SLATE, DictTagType.INFO),
    True_Client_IP("True-Client-IP", DictTagColor.SLATE, DictTagType.INFO);

    private final String label;
    private final DictTagColor tagColor;
    private final DictTagType tagType;

    ClientIpMode(String label, DictTagColor tagColor, DictTagType tagType) {
        this.label = label;
        this.tagColor = tagColor;
        this.tagType = tagType;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public String tagColor() {
        return tagColor.itemValue();
    }

    @Override
    public String tagType() {
        return tagType.itemValue();
    }
}
