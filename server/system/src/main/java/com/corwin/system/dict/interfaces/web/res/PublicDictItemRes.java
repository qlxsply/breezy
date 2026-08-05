package com.corwin.system.dict.interfaces.web.res;

/**
 * 公共字典项响应。
 *
 * @author Corwin 2026/8/5
 */
public record PublicDictItemRes(
        String itemCode,
        String itemLabel,
        String itemValue,
        String tagColor,
        String tagType
) {
}
